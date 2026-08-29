package com.example.candidateregistration.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.candidateregistration.model.TrainingSchedule;
import com.example.candidateregistration.model.TrainingSchedule.TrainingStatus;

@Repository
public interface TrainingScheduleRepository
        extends JpaRepository<TrainingSchedule, Long> {

    List<TrainingSchedule>
        findAllByOrderByStartDateDesc();

    List<TrainingSchedule>
        findByStatusOrderByStartDateDesc(
            TrainingStatus status
        );

    List<TrainingSchedule>
        findByTrainingTitleContainingIgnoreCaseOrTechnologyContainingIgnoreCaseOrTrainerNameContainingIgnoreCase(
            String trainingTitle,
            String technology,
            String trainerName
        );
}