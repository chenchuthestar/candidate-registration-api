package com.example.candidateregistration.repository;

import com.example.candidateregistration.model.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {
}
