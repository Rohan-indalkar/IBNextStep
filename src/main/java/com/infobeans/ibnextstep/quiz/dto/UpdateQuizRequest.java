package com.infobeans.ibnextstep.quiz.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateQuizRequest {
    private String title;

    @Min(1)
    private Integer durationMinutes;

    @DecimalMin("0") @DecimalMax("100")
    private Double passingPercentage;
}
