package com.example.candidateregistration.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.candidateregistration.dto.TrainingScheduleRequest;
import com.example.candidateregistration.model.TrainingSchedule;
import com.example.candidateregistration.model.TrainingSchedule.TrainingStatus;
import com.example.candidateregistration.service.TrainingScheduleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/trainings")
public class TrainingScheduleController {

	private final TrainingScheduleService trainingScheduleService;

	public TrainingScheduleController(TrainingScheduleService trainingScheduleService) {
		this.trainingScheduleService = trainingScheduleService;
	}

	/*
	 * GET /api/trainings
	 *
	 * GET /api/trainings?status=SCHEDULED
	 *
	 * GET /api/trainings?search=Java
	 */
	@GetMapping
	public ResponseEntity<List<TrainingSchedule>> getTrainings(@RequestParam(required = false) TrainingStatus status,
			@RequestParam(required = false) String search) {

		if (search != null && !search.isBlank()) {

			return ResponseEntity.ok(trainingScheduleService.searchTrainings(search.trim()));
		}

		if (status != null) {

			return ResponseEntity.ok(trainingScheduleService.getTrainingsByStatus(status));
		}

		return ResponseEntity.ok(trainingScheduleService.getAllTrainings());
	}

	/*
	 * GET /api/trainings/{id}
	 */
	@GetMapping("/{id}")
	public ResponseEntity<TrainingSchedule> getTrainingById(@PathVariable Long id) {

		return ResponseEntity.ok(trainingScheduleService.getTrainingById(id));
	}

	/*
	 * POST /api/trainings
	 */
	@PostMapping
	public ResponseEntity<TrainingSchedule> createTraining(@Valid @RequestBody TrainingScheduleRequest request) {

		TrainingSchedule savedTraining = trainingScheduleService.createTraining(request);

		return ResponseEntity.status(HttpStatus.CREATED).body(savedTraining);
	}

	/*
	 * PUT /api/trainings/{id}
	 */
	@PutMapping("/{id}")
	public ResponseEntity<TrainingSchedule> updateTraining(@PathVariable Long id,
			@Valid @RequestBody TrainingScheduleRequest request) {

		return ResponseEntity.ok(trainingScheduleService.updateTraining(id, request));
	}

	/*
	 * DELETE /api/trainings/{id}
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteTraining(@PathVariable Long id) {

		trainingScheduleService.deleteTraining(id);

		return ResponseEntity.noContent().build();
	}
}