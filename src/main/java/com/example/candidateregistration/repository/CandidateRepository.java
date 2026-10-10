package com.example.candidateregistration.repository;

import com.example.candidateregistration.model.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    interface NotificationRecipient {
        Long getId();
        String getFullName();
        String getEmail();
    }
    java.util.List<NotificationRecipient> findByActiveTrueAndJobCategoryAndCandidateType(
            com.example.candidateregistration.model.JobCategory category,
            com.example.candidateregistration.model.CandidateType candidateType);
}
