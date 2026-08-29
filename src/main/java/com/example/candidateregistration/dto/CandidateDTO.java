package com.example.candidateregistration.dto;

import java.util.List;

import com.example.candidateregistration.model.CandidateType;
import com.example.candidateregistration.model.JobCategory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateDTO {

    // Present only in responses; ignored/null on incoming create requests.
    private Long id;

    // Present only in responses (set by the server after the file is stored).
    private String resumeFileName;

    @NotBlank(message = "Full Name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    private String email;

    @NotBlank(message = "Mobile Number is required")
    @Pattern(regexp = "^\\d{10}$", message = "Mobile Number must contain exactly 10 digits")
    private String mobile;

    @NotNull(message = "Candidate Type is required")
    private CandidateType candidateType;

    @NotNull(message = "Job Category is required")
    private JobCategory jobCategory;

    @Valid
    @NotNull(message = "At least one education record is required")
    @Size(min = 1, message = "At least one education record is required")
    private List<EducationDetailDTO> educationDetails;

    @Valid
    private List<ExperienceDetailDTO> experienceDetails;
    
    private String referencedByName;
    private String referencedByNumber;
}
