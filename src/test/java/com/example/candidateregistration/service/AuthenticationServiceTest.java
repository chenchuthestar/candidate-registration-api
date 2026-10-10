package com.example.candidateregistration.service;

import com.example.candidateregistration.dto.*;
import com.example.candidateregistration.exception.InvalidRequestException;
import com.example.candidateregistration.model.*;
import com.example.candidateregistration.repository.*;
import com.example.candidateregistration.security.JwtProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Instant;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticationServiceTest {
    UserRepository users;
    RegistrationTokenRepository tokens;
    PasswordEncoder passwords;
    JwtProvider jwt;
    ApplicationEventPublisher events;
    AuthenticationService service;
    User admin;
    private static final String RAW = "A".repeat(43);
    @BeforeEach void setup() {
        users = mock(UserRepository.class); tokens = mock(RegistrationTokenRepository.class);
        passwords = mock(PasswordEncoder.class); jwt = mock(JwtProvider.class);
        events = mock(ApplicationEventPublisher.class);
        service = new AuthenticationService(users, tokens, passwords, jwt, events);
        ReflectionTestUtils.setField(service, "approverEmail", "reviewer@example.com");
        ReflectionTestUtils.setField(service, "expirationHours", 48L);
        admin = User.builder().id(1L).email("admin@example.com").password("hash").role("Admin").build();
    }
    @ParameterizedTest
    @CsvSource({"false,PENDING,false", "true,PENDING,false", "false,APPROVED,false",
            "true,APPROVED,true", "false,REJECTED,false", "true,REJECTED,false"})
    void requiresBothGates(boolean verified, AdminApprovalStatus status, boolean allowed) {
        admin.setEmailVerified(verified); admin.setAdminApprovalStatus(status);
        when(users.findByEmailIgnoreCase(admin.getEmail())).thenReturn(Optional.of(admin));
        when(passwords.matches("password", "hash")).thenReturn(true);
        when(jwt.generateToken(admin.getEmail())).thenReturn("jwt");
        LoginRequest request = new LoginRequest(admin.getEmail(), "password");
        if (allowed) assertEquals("jwt", service.login(request).getToken());
        else { assertThrows(InvalidRequestException.class, () -> service.login(request)); verifyNoInteractions(jwt); }
    }
    @Test void signupPersistsPendingAdminAndSeparateHashedTokens() {
        when(users.save(any())).thenAnswer(inv -> { User u = inv.getArgument(0); u.setId(1L); return u; });
        when(passwords.encode("Password@1")).thenReturn("hash");
        SignupRequest request = new SignupRequest("ADMIN@example.com", "Password@1", "Password@1", "Admin");
        SignupResponse response = service.signup(request);
        assertEquals("admin@example.com", response.getEmail());
        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class); verify(users).save(user.capture());
        assertFalse(user.getValue().canLogin()); assertNotNull(user.getValue().getCreatedAt());
        ArgumentCaptor<RegistrationToken> saved = ArgumentCaptor.forClass(RegistrationToken.class);
        verify(tokens, times(2)).save(saved.capture());
        ArgumentCaptor<AdminRegistrationEvent> event = ArgumentCaptor.forClass(AdminRegistrationEvent.class);
        verify(events).publishEvent(event.capture());
        assertNotEquals(event.getValue().verificationToken(), event.getValue().reviewToken());
        assertEquals(AuthenticationService.hash(event.getValue().verificationToken()), saved.getAllValues().get(0).getTokenHash());
        assertEquals("reviewer@example.com", saved.getAllValues().get(1).getReviewerEmail());
    }
    @Test void rejectsPasswordMismatchBeforePersistence() {
        assertThrows(InvalidRequestException.class, () -> service.signup(new SignupRequest("a@example.com", "Password@1", "other", "Admin")));
        verifyNoInteractions(users, tokens, events);
    }
    private RegistrationToken token(RegistrationToken.Purpose purpose) {
        RegistrationToken token = new RegistrationToken(); token.setUser(admin); token.setPurpose(purpose);
        token.setExpiresAt(Instant.now().plusSeconds(60)); token.setReviewerEmail("reviewer@example.com");
        when(tokens.findByTokenHash(AuthenticationService.hash(RAW))).thenReturn(Optional.of(token));
        when(users.findForUpdateById(1L)).thenReturn(Optional.of(admin));
        return token;
    }
    @Test void verificationConsumesTokenWithoutApprovingAdmin() {
        RegistrationToken t = token(RegistrationToken.Purpose.VERIFY_EMAIL);
        service.verifyEmail(RAW);
        assertTrue(admin.isEmailVerified()); assertNotNull(admin.getEmailVerifiedAt()); assertNotNull(t.getConsumedAt());
        assertEquals(AdminApprovalStatus.PENDING, admin.getAdminApprovalStatus()); assertFalse(admin.canLogin());
        assertThrows(InvalidRequestException.class, () -> service.verifyEmail(RAW));
    }
    @ParameterizedTest @CsvSource({"approve,APPROVED", "reject,REJECTED"})
    void reviewIsIndependentAndCannotBeReplayed(String decision, AdminApprovalStatus expected) {
        RegistrationToken t = token(RegistrationToken.Purpose.REVIEW_ADMIN);
        service.reviewAdmin(RAW, decision);
        assertEquals(expected, admin.getAdminApprovalStatus()); assertFalse(admin.canLogin());
        assertEquals("reviewer@example.com", admin.getAdminReviewedBy()); assertNotNull(t.getConsumedAt());
        assertThrows(InvalidRequestException.class, () -> service.reviewAdmin(RAW, "approve"));
    }
    @Test void rejectsExpiredAndWrongPurposeLinks() {
        RegistrationToken t = token(RegistrationToken.Purpose.REVIEW_ADMIN);
        assertThrows(InvalidRequestException.class, () -> service.verifyEmail(RAW));
        t.setExpiresAt(Instant.now().minusSeconds(1));
        assertThrows(InvalidRequestException.class, () -> service.reviewAdmin(RAW, "approve"));
        assertEquals(AdminApprovalStatus.PENDING, admin.getAdminApprovalStatus()); assertNull(t.getConsumedAt());
    }
    @Test void previewDoesNotConsumeOrChangeApproval() {
        RegistrationToken t = token(RegistrationToken.Purpose.REVIEW_ADMIN);
        assertEquals(admin.getEmail(), service.reviewEmail(RAW));
        assertNull(t.getConsumedAt()); assertEquals(AdminApprovalStatus.PENDING, admin.getAdminApprovalStatus());
    }
    @Test void wrongPasswordDoesNotIssueJwt() {
        admin.setEmailVerified(true); admin.setAdminApprovalStatus(AdminApprovalStatus.APPROVED);
        when(users.findByEmailIgnoreCase(admin.getEmail())).thenReturn(Optional.of(admin));
        assertThrows(InvalidRequestException.class, () -> service.login(new LoginRequest(admin.getEmail(), "wrong")));
        verifyNoInteractions(jwt);
    }
}
