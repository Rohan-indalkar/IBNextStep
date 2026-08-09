package com.infobeans.ibnextstep.codingassessment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.Instant;

@Data
public class UpdateAssessmentRequest {
    private String title;
    private String description;
    private Integer durationMinutes;
    private Instant startTime;
    private Instant endTime;
    private Double passingMarks;
    private Integer maxAttempts;
}
