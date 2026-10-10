package com.example.candidateregistration.service;

import com.example.candidateregistration.dto.CandidateDTO;
import java.time.Instant;

public record CandidateRegistrationEvent(CandidateDTO candidate, Instant registeredAt) {
    @Override public String toString() { return "CandidateRegistrationEvent[id=" + candidate.getId() + "]"; }
}
