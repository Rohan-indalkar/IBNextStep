package com.infobeans.ibnextstep.codingassessment.dto;

import com.infobeans.ibnextstep.codingassessment.ProgrammingLanguage;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class CreateAssessmentRequest {
    @NotBlank
    private String title;

    private String description;

    @NotBlank
    private String batchId;

    @Min(1)
    private int durationMinutes;

    @NotNull
    private Instant startTime;

    @NotNull
    private Instant endTime;

    @DecimalMin("0")
    private double passingMarks;

    @Min(1)
    private int maxAttempts = 1;

    @NotEmpty
    private List<ProgrammingLanguage> allowedLanguages;
}
