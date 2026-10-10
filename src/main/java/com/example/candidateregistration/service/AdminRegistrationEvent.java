package com.example.candidateregistration.service;
public record AdminRegistrationEvent(Long userId, String email, String approverEmail,
        String verificationToken, String reviewToken) {
    @Override public String toString() { return "AdminRegistrationEvent[userId=" + userId + "]"; }
}
