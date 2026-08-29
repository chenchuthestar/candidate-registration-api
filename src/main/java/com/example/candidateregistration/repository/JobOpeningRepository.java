package com.example.candidateregistration.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.candidateregistration.model.JobOpening;
import com.example.candidateregistration.model.JobOpening.JobStatus;

@Repository
public interface JobOpeningRepository extends JpaRepository<JobOpening, Long> {

    List<JobOpening> findAllByOrderByPostedDateDesc();

    List<JobOpening> findByStatusOrderByPostedDateDesc(JobStatus status);

    List<JobOpening> findByJobTitleContainingIgnoreCaseOrCompanyNameContainingIgnoreCase(
            String jobTitle,
            String companyName
    );
}