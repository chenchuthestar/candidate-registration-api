package com.example.candidateregistration.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
public class AdminRegistrationMailer {
    private static final Logger log = LoggerFactory.getLogger(AdminRegistrationMailer.class);
    private final EmailService emails;
    private final TemplateEngine templates;
    @Value("${app.registration.base-url}") private String baseUrl;
    @Value("${app.registration.token-expiration-hours:48}") private long expirationHours;

    public AdminRegistrationMailer(EmailService emails, TemplateEngine templates) {
        this.emails = emails;
        this.templates = templates;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void verification(AdminRegistrationEvent event) {
        Context context = context(event);
        context.setVariable("verificationUrl", baseUrl.replaceAll("/+$", "")
                + "/api/auth/verify-email?token=" + event.verificationToken());
        deliver(event.userId(), event.email(), "Verify your StarTech admin email",
                templates.process("emails/admin-email-verification", context));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void approval(AdminRegistrationEvent event) {
        Context context = context(event);
        String link = baseUrl.replaceAll("/+$", "") + "/api/auth/admin/review?token=" + event.reviewToken();
        context.setVariable("approveUrl", link + "&decision=approve");
        context.setVariable("rejectUrl", link + "&decision=reject");
        deliver(event.userId(), event.approverEmail(), "Review StarTech admin registration",
                templates.process("emails/admin-registration-template", context));
    }

    private Context context(AdminRegistrationEvent event) {
        Context context = new Context();
        context.setVariable("email", event.email());
        context.setVariable("registrationId", event.userId());
        context.setVariable("expirationHours", expirationHours);
        return context;
    }

    private void deliver(Long id, String recipient, String subject, String body) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                emails.sendRegistrationEmail(recipient, subject, body);
                return;
            } catch (Exception e) {
                // Never log the message body or registration tokens.
                log.warn("Registration email attempt {} failed for user {} ({})",
                        attempt, id, e.getClass().getSimpleName());
            }
        }
        log.error("Registration email delivery failed for user {} after three attempts; support intervention required", id);
    }
}

@Configuration
@EnableAsync
class RegistrationAsyncConfiguration { }
