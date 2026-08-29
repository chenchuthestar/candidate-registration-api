package com.example.candidateregistration.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.candidateregistration.dto.JobOpeningRequest;
import com.example.candidateregistration.model.JobOpening;
import com.example.candidateregistration.model.JobOpening.JobStatus;
import com.example.candidateregistration.service.JobOpeningService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/job-openings")
@CrossOrigin(origins = "http://localhost:5173")
public class JobOpeningController {

	private final JobOpeningService jobOpeningService;

	public JobOpeningController(JobOpeningService jobOpeningService) {
		this.jobOpeningService = jobOpeningService;
	}

	/*
	 * GET /api/job-openings GET /api/job-openings?status=OPEN GET
	 * /api/job-openings?search=Java
	 */
	@GetMapping
	public ResponseEntity<List<JobOpening>> getAllJobOpenings(@RequestParam(required = false) JobStatus status,
			@RequestParam(required = false) String search) {
		if (search != null && !search.isBlank()) {
			return ResponseEntity.ok(jobOpeningService.searchJobOpenings(search.trim()));
		}

		if (status != null) {
			return ResponseEntity.ok(jobOpeningService.getJobOpeningsByStatus(status));
		}

		return ResponseEntity.ok(jobOpeningService.getAllJobOpenings());
	}

	@GetMapping("/{id}")
	public ResponseEntity<JobOpening> getJobOpeningById(@PathVariable Long id) {
		return ResponseEntity.ok(jobOpeningService.getJobOpeningById(id));
	}

	@PostMapping
	public ResponseEntity<JobOpening> createJobOpening(@Valid @RequestBody JobOpeningRequest request) {
		JobOpening savedJob = jobOpeningService.createJobOpening(request);

		return ResponseEntity.status(HttpStatus.CREATED).body(savedJob);
	}

	@PutMapping("/{id}")
	public ResponseEntity<JobOpening> updateJobOpening(@PathVariable Long id,
			@Valid @RequestBody JobOpeningRequest request) {
		return ResponseEntity.ok(jobOpeningService.updateJobOpening(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteJobOpening(@PathVariable Long id) {
		jobOpeningService.deleteJobOpening(id);
		return ResponseEntity.noContent().build();
	}
}