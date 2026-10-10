package com.example.candidateregistration.service;

import com.example.candidateregistration.dto.*;
import com.example.candidateregistration.model.*;
import com.example.candidateregistration.repository.CandidateRepository;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class CandidateRegistrationMailerTest {
    private EmailService emails;
    private CandidateRegistrationMailer mailer;
    private CandidateDTO candidate;
    @BeforeEach void setup() {
        emails = mock(EmailService.class);
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/"); resolver.setSuffix(".html"); resolver.setTemplateMode("HTML");
        SpringTemplateEngine engine = new SpringTemplateEngine(); engine.setTemplateResolver(resolver);
        mailer = new CandidateRegistrationMailer(emails, engine);
        candidate = new CandidateDTO(); candidate.setId(42L); candidate.setFullName("Alex <script>alert(1)</script>");
        candidate.setEmail("alex@example.com"); candidate.setMobile("9876543210");
        candidate.setCandidateType(CandidateType.Fresher); candidate.setJobCategory(JobCategory.IT_Job);
        candidate.setResumeFileName("alex-resume.pdf");
        candidate.setEducationDetails(List.of(new EducationDetailDTO("B.Tech", "Example Institute", "Example University", "2024", "8.5")));
        candidate.setExperienceDetails(List.of());
    }
    @Test void confirmationIncludesSavedIdAndDetailsWithEscapedContent() throws Exception {
        mailer.registrationCompleted(new CandidateRegistrationEvent(candidate, Instant.parse("2026-10-10T10:00:00Z")));
        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(emails).sendRegistrationEmail(eq("alex@example.com"), eq("Registration successful - StarTech #42"), html.capture());
        String body = html.getValue();
        for (String detail : List.of("Registration successful!", "42", "9876543210", "alex-resume.pdf", "B.Tech", "Example Institute", "8.5"))
            assertTrue(body.contains(detail), detail);
        assertTrue(body.contains("&lt;script&gt;")); assertFalse(body.contains("<script>"));
        assertFalse(body.contains("${")); assertFalse(body.contains("Experience details"));
    }
    @Test void experiencedCandidateGetsCompanyDatesAndCtc() throws Exception {
        candidate.setCandidateType(CandidateType.Experienced);
        candidate.setExperienceDetails(List.of(new ExperienceDetailDTO("Example Company", "Developer", "2024-01-01", null, true, new BigDecimal("12.5"))));
        mailer.registrationCompleted(new CandidateRegistrationEvent(candidate, Instant.now()));
        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(emails).sendRegistrationEmail(anyString(), anyString(), html.capture());
        for (String detail : List.of("Experience details", "Example Company", "Developer", "2024-01-01", "Currently working", "12.5"))
            assertTrue(html.getValue().contains(detail), detail);
    }
    @Test void retriesSmtpFailureWithoutThrowingToRegistration() throws Exception {
        doThrow(new jakarta.mail.MessagingException("SMTP unavailable")).when(emails).sendRegistrationEmail(anyString(), anyString(), anyString());
        assertDoesNotThrow(() -> mailer.registrationCompleted(new CandidateRegistrationEvent(candidate, Instant.now())));
        verify(emails, times(3)).sendRegistrationEmail(anyString(), anyString(), anyString());
    }
    @Test void creationPublishesSavedDetailsAndDoesNotPublishWhenSaveFails() {
        CandidateRepository repository = mock(CandidateRepository.class);
        FileStorageService files = mock(FileStorageService.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        CandidateService service = new CandidateService(repository, files, events);
        when(files.storeResume(any())).thenReturn("stored.pdf");
        when(repository.save(any())).thenAnswer(inv -> { Candidate saved = inv.getArgument(0); saved.setId(99L); return saved; });
        MockMultipartFile resume = new MockMultipartFile("resume", "resume.pdf", "application/pdf", new byte[]{1});
        candidate.setId(null);
        CandidateDTO result = service.createCandidate(candidate, resume);
        ArgumentCaptor<CandidateRegistrationEvent> event = ArgumentCaptor.forClass(CandidateRegistrationEvent.class);
        verify(events).publishEvent(event.capture());
        assertEquals(99L, event.getValue().candidate().getId()); assertEquals(result, event.getValue().candidate());
        assertEquals("resume.pdf", event.getValue().candidate().getResumeFileName());
        clearInvocations(events);
        doThrow(new IllegalStateException("DB failed")).when(repository).save(any());
        assertThrows(IllegalStateException.class, () -> service.createCandidate(candidate, resume));
        verifyNoInteractions(events);
    }
    @Test void statusChangesPersistAndDoNotSendAnotherRegistrationEmail() {
        CandidateRepository repository = mock(CandidateRepository.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        CandidateService service = new CandidateService(repository, mock(FileStorageService.class), events);
        Candidate saved = Candidate.builder().id(99L).build();
        when(repository.findById(99L)).thenReturn(java.util.Optional.of(saved));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertTrue(saved.isActive());
        assertFalse(service.updateStatus(99L, false).isActive());
        assertFalse(saved.isActive());
        assertTrue(service.updateStatus(99L, true).isActive());
        verify(repository, times(2)).save(saved);
        verifyNoInteractions(events);
        assertThrows(com.example.candidateregistration.exception.ResourceNotFoundException.class,
                () -> service.updateStatus(404L, false));
    }
}
