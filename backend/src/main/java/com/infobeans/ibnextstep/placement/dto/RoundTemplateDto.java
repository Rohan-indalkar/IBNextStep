package com.infobeans.ibnextstep.placement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class RoundTemplateDto {

    @Positive(message = "Round number must be positive")
    private int roundNumber;

    @NotBlank(message = "Round name is required")
    private String name;
}
