package com.example.candidateregistration.security;

import com.example.candidateregistration.model.*;
import com.example.candidateregistration.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @ParameterizedTest @ValueSource(strings = {"Admin", "Recruiter"})
    void roleComesFromDatabaseRatherThanTheClient(String role) throws Exception {
        JwtProvider jwt = mock(JwtProvider.class);
        UserRepository users = mock(UserRepository.class);
        when(jwt.validateToken("token")).thenReturn(true);
        when(jwt.getEmailFromToken("token")).thenReturn("user@example.com");
        User user = User.builder().role(role).active("approved").emailVerified(true)
                .adminApprovalStatus(AdminApprovalStatus.APPROVED).build();
        when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/candidates/1/status");
        request.addHeader("Authorization", "Bearer token");
        new JwtAuthenticationFilter(jwt, users).doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + role)));
        if (!role.equals("Admin")) assertFalse(authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_Admin")));
    }
}
