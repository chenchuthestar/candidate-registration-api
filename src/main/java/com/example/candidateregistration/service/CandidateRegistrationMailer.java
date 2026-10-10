package com.example.candidateregistration.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class CandidateRegistrationMailer {
    private static final Logger log = LoggerFactory.getLogger(CandidateRegistrationMailer.class);
    private final EmailService emails;
    private final TemplateEngine templates;

    public CandidateRegistrationMailer(EmailService emails, TemplateEngine templates) {
        this.emails = emails;
        this.templates = templates;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void registrationCompleted(CandidateRegistrationEvent event) {
        Context context = new Context();
        context.setVariable("candidate", event.candidate());
        context.setVariable("registrationDate", DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm 'UTC'")
                .withZone(ZoneOffset.UTC).format(event.registeredAt()));
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                String body = templates.process("emails/candidate-registration-success", context);
                emails.sendRegistrationEmail(event.candidate().getEmail(),
                        "Registration successful - StarTech #" + event.candidate().getId(), body);
                return;
            } catch (Exception e) {
                log.warn("Candidate confirmation email attempt {} failed for registration {} ({})",
                        attempt, event.candidate().getId(), e.getClass().getSimpleName());
            }
        }
        log.error("Candidate confirmation email failed for registration {} after three attempts",
                event.candidate().getId());
    }
}
