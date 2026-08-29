package com.example.candidateregistration.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "candidates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 10)
    private String mobile;

    // Original filename shown to users (e.g. "ravi_resume.pdf").
    @Column(length = 255)
    private String resumeFileName;

    // Server-generated unique filename actually stored on disk — never trust the original name for storage.
    @Column(length = 255, unique = true)
    private String resumeStoredPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CandidateType candidateType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobCategory jobCategory;

    @Builder.Default
    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<EducationDetail> educationDetails = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ExperienceDetail> experienceDetails = new ArrayList<>();

    @Column(name = "referenced_by_name", length = 100)
    private String referencedByName;

    @Column(name = "referenced_by_number", length = 20)
    private String referencedByNumber;
    
    public void addEducationDetail(EducationDetail detail) {
        educationDetails.add(detail);
        detail.setCandidate(this);
    }

    public void addExperienceDetail(ExperienceDetail detail) {
        experienceDetails.add(detail);
        detail.setCandidate(this);
    }
}
