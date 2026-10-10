package com.example.candidateregistration.service;

import com.example.candidateregistration.model.*;
import com.example.candidateregistration.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.slf4j.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Component
public class JobOpeningMailer {
    private static final Logger log = LoggerFactory.getLogger(JobOpeningMailer.class);
    private final JobOpeningRepository jobs;
    private final CandidateRepository candidates;
    private final EmailService emails;
    private final TemplateEngine templates;
    @Value("${app.jobs.whatsapp-number:919618254711}") private String whatsappNumber;

    public JobOpeningMailer(JobOpeningRepository jobs, CandidateRepository candidates, EmailService emails, TemplateEngine templates) {
        this.jobs = jobs; this.candidates = candidates; this.emails = emails; this.templates = templates;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyCandidates(JobOpeningPostedEvent event) {
        JobOpening job = jobs.findById(event.jobId()).orElse(null);
        if (job == null || job.getStatus() != JobOpening.JobStatus.OPEN || job.getCandidateType() == null) return;
        if (job.getClosingDate() != null && job.getClosingDate().isBefore(LocalDate.now(ZoneId.of("Asia/Kolkata")))) return;
        JobCategory category = category(job.getJobCategory());
        if (category == null) { log.warn("Job {} has no recognized category; notification skipped", event.jobId()); return; }
        if (whatsappNumber == null || !whatsappNumber.matches("[1-9][0-9]{7,14}")) {
            log.error("Invalid app.jobs.whatsapp-number; job {} notification skipped", event.jobId()); return;
        }
        Set<String> sent = new HashSet<>();
        for (var candidate : candidates.findByActiveTrueAndJobCategoryAndCandidateType(category, job.getCandidateType())) {
            if (candidate.getEmail() == null || candidate.getEmail().isBlank()) continue;
            String email = candidate.getEmail().trim();
            if (!sent.add(email.toLowerCase(Locale.ROOT))) continue;
            String message = "Hello StarTech, I am interested in the " + job.getJobTitle() + " job opening at "
                    + job.getCompanyName() + " (Job ID: " + job.getId() + "). My name is "
                    + candidate.getFullName() + ", Registration ID: " + candidate.getId() + ".";
            Context context = new Context();
            context.setVariable("job", job); context.setVariable("candidateName", candidate.getFullName());
            context.setVariable("category", category == JobCategory.IT_Job ? "IT Job" : "Non-IT Job");
            context.setVariable("whatsappNumber", "+" + whatsappNumber);
            context.setVariable("whatsappUrl", "https://wa.me/" + whatsappNumber + "?text="
                    + URLEncoder.encode(message, StandardCharsets.UTF_8).replace("+", "%20"));
            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    emails.sendRegistrationEmail(email, "New job opening: " + job.getJobTitle() + " - StarTech",
                            templates.process("emails/job-opening-notification", context));
                    break;
                } catch (Exception e) {
                    log.warn("Job {} email attempt {} failed for candidate {} ({})", job.getId(), attempt, candidate.getId(), e.getClass().getSimpleName());
                    if (attempt == 3) log.error("Job {} email delivery failed for candidate {}", job.getId(), candidate.getId());
                }
            }
        }
    }

    static JobCategory category(String value) {
        if (value == null) return null;
        return switch (value.trim().toUpperCase(Locale.ROOT).replace('-', '_')) {
            case "IT", "IT_JOB" -> JobCategory.IT_Job;
            case "NON_IT", "NON_IT_JOB" -> JobCategory.Non_IT_Job;
            default -> null;
        };
    }
}
