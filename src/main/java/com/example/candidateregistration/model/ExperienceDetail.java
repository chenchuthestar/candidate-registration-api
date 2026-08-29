package com.example.candidateregistration.model;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "experience_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExperienceDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String companyName;

    @Column(nullable = false, length = 150)
    private String designation;

    // Stored as ISO date strings ("yyyy-MM-dd") to match the frontend's <input type="date"> values.
    @Column(nullable = false, length = 10)
    private String fromDate;

    @Column(length = 10)
    private String toDate;

    @Column(nullable = false)
    private boolean currentlyWorking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    @JsonBackReference
    private Candidate candidate;
    
    @Column(name = "ctc", precision = 12, scale = 2)
    private BigDecimal ctc;

}
