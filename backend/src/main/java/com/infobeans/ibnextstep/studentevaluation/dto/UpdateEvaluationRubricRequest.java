package com.infobeans.ibnextstep.studentevaluation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEvaluationRubricRequest {

    @NotEmpty(message = "At least one skill is required")
    private List<@NotBlank String> skills;
}
