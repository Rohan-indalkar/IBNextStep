package com.infobeans.ibnextstep.batch.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssignTrainerRequest {
    @NotBlank
    private String trainerId;
}
