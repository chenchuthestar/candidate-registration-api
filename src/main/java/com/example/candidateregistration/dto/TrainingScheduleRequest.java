package com.example.candidateregistration.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.example.candidateregistration.model.TrainingSchedule.TrainingMode;
import com.example.candidateregistration.model.TrainingSchedule.TrainingStatus;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TrainingScheduleRequest {

	@NotBlank(message = "Training title is required")
	@Size(max = 150, message = "Training title cannot exceed 150 characters")
	private String trainingTitle;

	@NotBlank(message = "Technology / Course is required")
	@Size(max = 150, message = "Technology cannot exceed 150 characters")
	private String technology;

	@NotBlank(message = "Trainer name is required")
	@Size(max = 120, message = "Trainer name cannot exceed 120 characters")
	private String trainerName;

	@NotNull(message = "Start date is required")
	private LocalDate startDate;

	@NotNull(message = "End date is required")
	private LocalDate endDate;

	@NotNull(message = "Start time is required")
	private LocalTime startTime;

	@NotNull(message = "End time is required")
	private LocalTime endTime;

	@NotNull(message = "Training mode is required")
	private TrainingMode mode;

	@Size(max = 500, message = "Location / Meeting link cannot exceed 500 characters")
	private String locationOrLink;

	@Min(value = 1, message = "Maximum participants must be at least 1")
	private Integer maxParticipants;

	@Size(max = 1000, message = "Description cannot exceed 1000 characters")
	private String description;

	@NotNull(message = "Training status is required")
	private TrainingStatus status;

	public String getTrainingTitle() {
		return trainingTitle;
	}

	public void setTrainingTitle(String trainingTitle) {
		this.trainingTitle = trainingTitle;
	}

	public String getTechnology() {
		return technology;
	}

	public void setTechnology(String technology) {
		this.technology = technology;
	}

	public String getTrainerName() {
		return trainerName;
	}

	public void setTrainerName(String trainerName) {
		this.trainerName = trainerName;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public LocalTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalTime startTime) {
		this.startTime = startTime;
	}

	public LocalTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalTime endTime) {
		this.endTime = endTime;
	}

	public TrainingMode getMode() {
		return mode;
	}

	public void setMode(TrainingMode mode) {
		this.mode = mode;
	}

	public String getLocationOrLink() {
		return locationOrLink;
	}

	public void setLocationOrLink(String locationOrLink) {
		this.locationOrLink = locationOrLink;
	}

	public Integer getMaxParticipants() {
		return maxParticipants;
	}

	public void setMaxParticipants(Integer maxParticipants) {
		this.maxParticipants = maxParticipants;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public TrainingStatus getStatus() {
		return status;
	}

	public void setStatus(TrainingStatus status) {
		this.status = status;
	}
}