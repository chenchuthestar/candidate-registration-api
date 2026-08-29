package com.example.candidateregistration.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "education_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EducationDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String qualification;

    @Column(nullable = false, length = 150)
    private String institutionName;

    @Column(nullable = false, length = 150)
    private String boardOrUniversity;

    @Column(nullable = false, length = 4)
    private String yearOfPassing;

    @Column(nullable = false, length = 20)
    private String score;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    @JsonBackReference
    private Candidate candidate;
}
