package com.example.candidateregistration.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceDetailDTO {

    @NotBlank(message = "Company Name is required")
    private String companyName;

    @NotBlank(message = "Designation is required")
    private String designation;

    @NotBlank(message = "From Date is required")
    private String fromDate;

    // Not @NotBlank here on purpose: only required when currentlyWorking is false.
    // That conditional rule is enforced in CandidateService rather than via annotations.
    private String toDate;

    private boolean currentlyWorking;
    private BigDecimal ctc;
}
