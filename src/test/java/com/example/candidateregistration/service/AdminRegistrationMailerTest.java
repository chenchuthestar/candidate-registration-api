package com.example.candidateregistration.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminRegistrationMailerTest {
    private EmailService emails;
    private AdminRegistrationMailer mailer;
    private final AdminRegistrationEvent event = new AdminRegistrationEvent(7L, "admin@example.com",
            "configured-reviewer@example.com", "verification-token", "review-token");
    @BeforeEach void setup() {
        emails = mock(EmailService.class);
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/"); resolver.setSuffix(".html"); resolver.setTemplateMode("HTML");
        SpringTemplateEngine engine = new SpringTemplateEngine(); engine.setTemplateResolver(resolver);
        mailer = new AdminRegistrationMailer(emails, engine);
        ReflectionTestUtils.setField(mailer, "baseUrl", "https://portal.example.com/");
        ReflectionTestUtils.setField(mailer, "expirationHours", 48L);
    }
    @Test void rendersVerificationToApplicantAndReviewToConfiguredApprover() throws Exception {
        mailer.verification(event); mailer.approval(event);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emails).sendRegistrationEmail(eq(event.email()), eq("Verify your StarTech admin email"), body.capture());
        assertTrue(body.getValue().contains("https://portal.example.com/api/auth/verify-email?token=verification-token"));
        assertFalse(body.getValue().contains("${"));
        verify(emails).sendRegistrationEmail(eq(event.approverEmail()), eq("Review StarTech admin registration"), body.capture());
        assertTrue(body.getValue().contains("review-token&amp;decision=approve"));
        assertTrue(body.getValue().contains("review-token&amp;decision=reject"));
        assertTrue(body.getValue().contains("admin@example.com"));
        assertFalse(body.getValue().contains("${"));
    }
    @Test void failedVerificationDeliveryDoesNotPreventApprovalEmail() throws Exception {
        doThrow(new jakarta.mail.MessagingException("SMTP unavailable"))
                .when(emails).sendRegistrationEmail(eq(event.email()), anyString(), anyString());
        mailer.verification(event); mailer.approval(event);
        verify(emails, times(3)).sendRegistrationEmail(eq(event.email()), anyString(), anyString());
        verify(emails).sendRegistrationEmail(eq(event.approverEmail()), anyString(), anyString());
    }
}
