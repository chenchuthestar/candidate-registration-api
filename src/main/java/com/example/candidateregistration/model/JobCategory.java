package com.example.candidateregistration.model;

public enum JobCategory {
    IT_Job("Looking for IT Job"),
    Non_IT_Job("Looking for Non-IT Job");

    private final String label;

    JobCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
