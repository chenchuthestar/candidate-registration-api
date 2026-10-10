package com.example.candidateregistration.service;

import com.example.candidateregistration.model.*;
import com.example.candidateregistration.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import java.util.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class JobOpeningMailerTest {
    JobOpeningRepository jobs;
    CandidateRepository candidates;
    EmailService emails;
    JobOpeningMailer mailer;
    JobOpening job;
    @BeforeEach void setup() {
        jobs = mock(JobOpeningRepository.class); candidates = mock(CandidateRepository.class); emails = mock(EmailService.class);
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/"); resolver.setSuffix(".html"); resolver.setTemplateMode("HTML");
        SpringTemplateEngine templates = new SpringTemplateEngine(); templates.setTemplateResolver(resolver);
        mailer = new JobOpeningMailer(jobs, candidates, emails, templates);
        ReflectionTestUtils.setField(mailer, "whatsappNumber", "919618254711");
        job = new JobOpening(); job.setId(8L); job.setJobTitle("Java & React Developer"); job.setCompanyName("Example Company");
        job.setLocation("Hyderabad"); job.setJobType("Full Time"); job.setJobCategory("IT");
        job.setCandidateType(CandidateType.Fresher); job.setStatus(JobOpening.JobStatus.OPEN);
        job.setDescription("<script>unsafe</script>");
        when(jobs.findById(8L)).thenReturn(Optional.of(job));
    }
    CandidateRepository.NotificationRecipient recipient(Long id, String email) {
        return new CandidateRepository.NotificationRecipient() {
            public Long getId() { return id; }
            public String getFullName() { return "Alex"; }
            public String getEmail() { return email; }
        };
    }
    @ParameterizedTest @CsvSource({"IT,IT_Job,Fresher", "NON_IT,Non_IT_Job,Experienced", "IT_Job,IT_Job,Experienced", "Non_IT_Job,Non_IT_Job,Fresher"})
    void matchesActiveCandidatesByBothCategoryAndType(String value, JobCategory category, CandidateType type) throws Exception {
        job.setJobCategory(value); job.setCandidateType(type);
        when(candidates.findByActiveTrueAndJobCategoryAndCandidateType(category, type)).thenReturn(List.of(recipient(12L, "alex@example.com")));
        mailer.notifyCandidates(new JobOpeningPostedEvent(8L));
        verify(candidates).findByActiveTrueAndJobCategoryAndCandidateType(category, type);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emails).sendRegistrationEmail(eq("alex@example.com"), anyString(), body.capture());
        assertTrue(body.getValue().contains("https://wa.me/919618254711?text="));
        String html = body.getValue();
        int textStart = html.indexOf("?text=") + 6;
        String decoded = URLDecoder.decode(html.substring(textStart, html.indexOf('"', textStart)), StandardCharsets.UTF_8);
        assertTrue(decoded.contains("I am interested in the Java & React Developer"));
        assertTrue(decoded.contains("Job ID: 8")); assertTrue(decoded.contains("Registration ID: 12"));
        assertFalse(body.getValue().contains("<script>")); assertFalse(body.getValue().contains("${"));
    }
    @Test void deduplicatesEmailsAndContinuesAfterRecipientFailure() throws Exception {
        when(candidates.findByActiveTrueAndJobCategoryAndCandidateType(JobCategory.IT_Job, CandidateType.Fresher))
            .thenReturn(List.of(recipient(1L,"fail@example.com"), recipient(2L,"ok@example.com"), recipient(3L,"OK@example.com")));
        doThrow(new jakarta.mail.MessagingException("SMTP failed")).when(emails).sendRegistrationEmail(eq("fail@example.com"), anyString(), anyString());
        mailer.notifyCandidates(new JobOpeningPostedEvent(8L));
        verify(emails, times(3)).sendRegistrationEmail(eq("fail@example.com"), anyString(), anyString());
        verify(emails).sendRegistrationEmail(eq("ok@example.com"), anyString(), anyString());
        verify(emails, never()).sendRegistrationEmail(eq("OK@example.com"), anyString(), anyString());
    }
    @Test void skipsClosedExpiredAndUnknownCategoryJobs() {
        job.setStatus(JobOpening.JobStatus.CLOSED); mailer.notifyCandidates(new JobOpeningPostedEvent(8L));
        job.setStatus(JobOpening.JobStatus.OPEN); job.setClosingDate(java.time.LocalDate.now().minusDays(2));
        mailer.notifyCandidates(new JobOpeningPostedEvent(8L));
        job.setClosingDate(null); job.setJobCategory("OTHER"); mailer.notifyCandidates(new JobOpeningPostedEvent(8L));
        verifyNoInteractions(candidates, emails);
    }
}
