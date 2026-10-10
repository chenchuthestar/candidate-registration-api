package com.example.candidateregistration.service;

import com.example.candidateregistration.dto.*;
import com.example.candidateregistration.exception.InvalidRequestException;
import com.example.candidateregistration.model.*;
import com.example.candidateregistration.repository.*;
import com.example.candidateregistration.security.JwtProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;

@Service
public class AuthenticationService {
    private final UserRepository users;
    private final RegistrationTokenRepository tokens;
    private final PasswordEncoder passwords;
    private final JwtProvider jwt;
    private final ApplicationEventPublisher events;
    private final SecureRandom random = new SecureRandom();
    @Value("${app.registration.approver-email}") private String approverEmail;
    @Value("${app.registration.token-expiration-hours:48}") private long expirationHours;

    public AuthenticationService(UserRepository users, RegistrationTokenRepository tokens,
            PasswordEncoder passwords, JwtProvider jwt, ApplicationEventPublisher events) {
        this.users = users; this.tokens = tokens; this.passwords = passwords;
        this.jwt = jwt; this.events = events;
    }

    public LoginResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.getEmail().trim())
                .orElseThrow(() -> new InvalidRequestException("Invalid email or password"));
        if (!passwords.matches(request.getPassword(), user.getPassword()))
            throw new InvalidRequestException("Invalid email or password");
        if ("Admin".equals(user.getRole())) {
            if (user.getAdminApprovalStatus() == AdminApprovalStatus.REJECTED)
                throw new InvalidRequestException("Your admin registration has been rejected.");
            if (!user.isEmailVerified())
                throw new InvalidRequestException("Please verify your email using the link sent to your inbox.");
            if (user.getAdminApprovalStatus() != AdminApprovalStatus.APPROVED)
                throw new InvalidRequestException("Your admin registration is awaiting approval.");
        }
        if (!user.canLogin()) throw new InvalidRequestException("Your account is pending. Please contact support.");
        return new LoginResponse(jwt.generateToken(user.getEmail()), user.getEmail(), "Login successful");
    }

    public java.util.Map<String, String> profile(String email) {
        User user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new InvalidRequestException("Account not found"));
        return java.util.Map.of("email", user.getEmail(), "role", user.getRole());
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword()))
            throw new InvalidRequestException("Passwords must match");
        if (!Set.of("HR", "Recruiter", "Manager", "Admin", "Candidate").contains(request.getRole()))
            throw new InvalidRequestException("Select a valid role");
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (users.findByEmailIgnoreCase(email).isPresent())
            throw new InvalidRequestException("Email already registered");
        boolean admin = "Admin".equals(request.getRole());
        User user = users.save(User.builder().email(email).password(passwords.encode(request.getPassword()))
                .role(request.getRole()).active(admin ? "pending" : "approved").build());
        if (admin) {
            String verification = issueToken(user, RegistrationToken.Purpose.VERIFY_EMAIL, null);
            String review = issueToken(user, RegistrationToken.Purpose.REVIEW_ADMIN, approverEmail);
            events.publishEvent(new AdminRegistrationEvent(user.getId(), email, approverEmail, verification, review));
        }
        return new SignupResponse(email, user.getRole(), admin
                ? "Account registered. Check your email to verify your address. You can log in once your email is verified and your admin registration is approved."
                : "Account created successfully. Please sign in.");
    }

    private String issueToken(User user, RegistrationToken.Purpose purpose, String reviewer) {
        if (expirationHours < 1 || expirationHours > 720)
            throw new IllegalStateException("Registration token expiration must be between 1 and 720 hours");
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RegistrationToken token = new RegistrationToken();
        token.setTokenHash(hash(raw)); token.setUser(user); token.setPurpose(purpose);
        token.setExpiresAt(Instant.now().plusSeconds(expirationHours * 3600));
        token.setReviewerEmail(reviewer); tokens.save(token);
        return raw;
    }

    @Transactional
    public String verifyEmail(String raw) {
        RegistrationToken token = validToken(raw, RegistrationToken.Purpose.VERIFY_EMAIL);
        User user = lockedUser(token);
        user.setEmailVerified(true); user.setEmailVerifiedAt(Instant.now());
        token.setConsumedAt(Instant.now());
        return "Email verified successfully. You can sign in after your admin registration is approved.";
    }

    @Transactional
    public String reviewEmail(String raw) {
        return validToken(raw, RegistrationToken.Purpose.REVIEW_ADMIN).getUser().getEmail();
    }

    @Transactional
    public String reviewAdmin(String raw, String decision) {
        if (!Set.of("approve", "reject").contains(decision))
            throw new InvalidRequestException("Invalid review decision");
        RegistrationToken token = validToken(raw, RegistrationToken.Purpose.REVIEW_ADMIN);
        User user = lockedUser(token);
        if (user.getAdminApprovalStatus() != AdminApprovalStatus.PENDING)
            throw new InvalidRequestException("This registration has already been reviewed");
        boolean approved = "approve".equals(decision);
        user.setAdminApprovalStatus(approved ? AdminApprovalStatus.APPROVED : AdminApprovalStatus.REJECTED);
        user.setActive(approved ? "approved" : "rejected");
        user.setAdminReviewedAt(Instant.now()); user.setAdminReviewedBy(token.getReviewerEmail());
        token.setConsumedAt(Instant.now());
        return approved ? "Admin registration approved. Email verification is also required before login."
                : "Admin registration rejected.";
    }

    private User lockedUser(RegistrationToken token) {
        User user = users.findForUpdateById(token.getUser().getId())
                .orElseThrow(() -> new InvalidRequestException("Registration no longer exists"));
        if (!"Admin".equals(user.getRole())) throw new InvalidRequestException("Invalid registration");
        return user;
    }

    private RegistrationToken validToken(String raw, RegistrationToken.Purpose purpose) {
        if (raw == null || !raw.matches("[A-Za-z0-9_-]{43}"))
            throw new InvalidRequestException("Invalid registration link");
        RegistrationToken token = tokens.findByTokenHash(hash(raw))
                .orElseThrow(() -> new InvalidRequestException("Invalid registration link"));
        if (token.getPurpose() != purpose || token.getConsumedAt() != null)
            throw new InvalidRequestException("This registration link is invalid or has already been used");
        if (!token.getExpiresAt().isAfter(Instant.now()))
            throw new InvalidRequestException("This registration link has expired. Please contact support.");
        return token;
    }

    static String hash(String raw) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(raw.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
