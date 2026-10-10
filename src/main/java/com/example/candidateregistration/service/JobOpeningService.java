package com.example.candidateregistration.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.candidateregistration.dto.JobOpeningRequest;
import com.example.candidateregistration.exception.ResourceNotFoundException;
import com.example.candidateregistration.model.JobOpening;
import com.example.candidateregistration.model.JobOpening.JobStatus;
import com.example.candidateregistration.repository.JobOpeningRepository;

@Service
@Transactional
public class JobOpeningService {

	private final JobOpeningRepository jobOpeningRepository;
	private final org.springframework.context.ApplicationEventPublisher events;

	public JobOpeningService(JobOpeningRepository jobOpeningRepository, org.springframework.context.ApplicationEventPublisher events) {
		this.jobOpeningRepository = jobOpeningRepository;
		this.events = events;
	}

	@Transactional(readOnly = true)
	public List<JobOpening> getAllJobOpenings() {
		return jobOpeningRepository.findAllByOrderByPostedDateDesc();
	}

	@Transactional(readOnly = true)
	public List<JobOpening> getJobOpeningsByStatus(JobStatus status) {
		return jobOpeningRepository.findByStatusOrderByPostedDateDesc(status);
	}

	@Transactional(readOnly = true)
	public List<JobOpening> searchJobOpenings(String search) {
		return jobOpeningRepository.findByJobTitleContainingIgnoreCaseOrCompanyNameContainingIgnoreCase(search, search);
	}

	@Transactional(readOnly = true)
	public JobOpening getJobOpeningById(Long id) {
		return jobOpeningRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Job opening not found with ID: " + id));
	}

	public JobOpening createJobOpening(JobOpeningRequest request) {
		validateExperience(request);
		validateDates(request);

		JobOpening jobOpening = new JobOpening();
		mapRequestToEntity(request, jobOpening);

		if (jobOpening.getPostedDate() == null) {
			jobOpening.setPostedDate(LocalDate.now());
		}

		if (jobOpening.getStatus() == null) {
			jobOpening.setStatus(JobStatus.OPEN);
		}

		JobOpening saved = jobOpeningRepository.save(jobOpening);
		if (saved.getStatus() == JobStatus.OPEN) events.publishEvent(new JobOpeningPostedEvent(saved.getId()));
		return saved;
	}

	public JobOpening updateJobOpening(Long id, JobOpeningRequest request) {
		validateExperience(request);
		validateDates(request);

		JobOpening existingJobOpening = getJobOpeningById(id);
		boolean wasOpen = existingJobOpening.getStatus() == JobStatus.OPEN;

		mapRequestToEntity(request, existingJobOpening);

		JobOpening saved = jobOpeningRepository.save(existingJobOpening);
		if (!wasOpen && saved.getStatus() == JobStatus.OPEN) events.publishEvent(new JobOpeningPostedEvent(saved.getId()));
		return saved;
	}

	public void deleteJobOpening(Long id) {
		JobOpening jobOpening = getJobOpeningById(id);
		jobOpeningRepository.delete(jobOpening);
	}

	private void mapRequestToEntity(JobOpeningRequest request, JobOpening jobOpening) {
		jobOpening.setCandidateType(request.getCandidateType());
		jobOpening.setJobTitle(request.getJobTitle());
		jobOpening.setCompanyName(request.getCompanyName());
		jobOpening.setLocation(request.getLocation());
		jobOpening.setMinExperience(request.getMinExperience());
		jobOpening.setMaxExperience(request.getMaxExperience());
		if (request.getCandidateType() == com.example.candidateregistration.model.CandidateType.Fresher) {
			jobOpening.setMinExperience(null);
			jobOpening.setMaxExperience(null);
		}
		jobOpening.setJobType(request.getJobType());
		jobOpening.setJobCategory(request.getJobCategory());
		jobOpening.setDescription(request.getDescription());
		jobOpening.setRequiredSkills(request.getRequiredSkills());
		jobOpening.setStatus(request.getStatus());
		jobOpening.setPostedDate(request.getPostedDate());
		jobOpening.setClosingDate(request.getClosingDate());
	}

	private void validateExperience(JobOpeningRequest request) {
		if (request.getCandidateType() == com.example.candidateregistration.model.CandidateType.Fresher) return;
		Integer minimum = request.getMinExperience();
		Integer maximum = request.getMaxExperience();

		if (minimum != null && maximum != null && maximum < minimum) {
			throw new IllegalArgumentException("Maximum experience cannot be less than minimum experience");
		}
	}

	private void validateDates(JobOpeningRequest request) {
		LocalDate postedDate = request.getPostedDate();
		LocalDate closingDate = request.getClosingDate();

		if (postedDate != null && closingDate != null && closingDate.isBefore(postedDate)) {
			throw new IllegalArgumentException("Closing date cannot be before posted date");
		}
	}
}
