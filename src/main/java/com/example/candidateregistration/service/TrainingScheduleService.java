package com.example.candidateregistration.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.candidateregistration.dto.TrainingScheduleRequest;
import com.example.candidateregistration.exception.ResourceNotFoundException;
import com.example.candidateregistration.model.TrainingSchedule;
import com.example.candidateregistration.model.TrainingSchedule.TrainingStatus;
import com.example.candidateregistration.repository.TrainingScheduleRepository;

@Service
@Transactional
public class TrainingScheduleService {

	private final TrainingScheduleRepository trainingScheduleRepository;

	public TrainingScheduleService(TrainingScheduleRepository trainingScheduleRepository) {
		this.trainingScheduleRepository = trainingScheduleRepository;
	}

	@Transactional(readOnly = true)
	public List<TrainingSchedule> getAllTrainings() {

		return trainingScheduleRepository.findAllByOrderByStartDateDesc();
	}

	@Transactional(readOnly = true)
	public TrainingSchedule getTrainingById(Long id) {

		return trainingScheduleRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Training schedule not found with ID: " + id));
	}

	@Transactional(readOnly = true)
	public List<TrainingSchedule> getTrainingsByStatus(TrainingStatus status) {

		return trainingScheduleRepository.findByStatusOrderByStartDateDesc(status);
	}

	@Transactional(readOnly = true)
	public List<TrainingSchedule> searchTrainings(String search) {

		return trainingScheduleRepository
				.findByTrainingTitleContainingIgnoreCaseOrTechnologyContainingIgnoreCaseOrTrainerNameContainingIgnoreCase(
						search, search, search);
	}

	public TrainingSchedule createTraining(TrainingScheduleRequest request) {

		validateTraining(request);

		TrainingSchedule training = new TrainingSchedule();

		mapRequestToEntity(request, training);

		return trainingScheduleRepository.save(training);
	}

	public TrainingSchedule updateTraining(Long id, TrainingScheduleRequest request) {

		validateTraining(request);

		TrainingSchedule training = getTrainingById(id);

		mapRequestToEntity(request, training);

		return trainingScheduleRepository.save(training);
	}

	public void deleteTraining(Long id) {

		TrainingSchedule training = getTrainingById(id);

		trainingScheduleRepository.delete(training);
	}

	private void mapRequestToEntity(TrainingScheduleRequest request, TrainingSchedule training) {

		training.setTrainingTitle(request.getTrainingTitle().trim());

		training.setTechnology(request.getTechnology().trim());

		training.setTrainerName(request.getTrainerName().trim());

		training.setStartDate(request.getStartDate());

		training.setEndDate(request.getEndDate());

		training.setStartTime(request.getStartTime());

		training.setEndTime(request.getEndTime());

		training.setMode(request.getMode());

		training.setLocationOrLink(trimToNull(request.getLocationOrLink()));

		training.setMaxParticipants(request.getMaxParticipants());

		training.setDescription(trimToNull(request.getDescription()));

		training.setStatus(request.getStatus());
	}

	private void validateTraining(TrainingScheduleRequest request) {

		LocalDate startDate = request.getStartDate();

		LocalDate endDate = request.getEndDate();

		if (startDate != null && endDate != null && endDate.isBefore(startDate)) {

			throw new IllegalArgumentException("End date cannot be before start date");
		}

		if (startDate != null && endDate != null && startDate.equals(endDate)) {

			LocalTime startTime = request.getStartTime();

			LocalTime endTime = request.getEndTime();

			if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {

				throw new IllegalArgumentException("End time must be after start time " + "for a same-day training");
			}
		}

		if (request.getMaxParticipants() != null && request.getMaxParticipants() < 1) {

			throw new IllegalArgumentException("Maximum participants must be at least 1");
		}
	}

	private String trimToNull(String value) {

		if (value == null) {
			return null;
		}

		String trimmed = value.trim();

		return trimmed.isEmpty() ? null : trimmed;
	}
}