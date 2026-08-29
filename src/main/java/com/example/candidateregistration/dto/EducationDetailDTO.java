package com.example.candidateregistration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EducationDetailDTO {

    @NotBlank(message = "Qualification is required")
    private String qualification;

    @NotBlank(message = "Institution Name is required")
    private String institutionName;

    @NotBlank(message = "Board or University is required")
    private String boardOrUniversity;

    @NotBlank(message = "Year of Passing is required")
    @Pattern(regexp = "^\\d{4}$", message = "Year of Passing must be a valid 4-digit year")
    private String yearOfPassing;

    @NotBlank(message = "Percentage or CGPA is required")
    private String score;
}
