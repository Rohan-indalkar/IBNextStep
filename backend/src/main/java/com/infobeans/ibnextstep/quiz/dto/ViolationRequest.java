package com.infobeans.ibnextstep.quiz.dto;

import com.infobeans.ibnextstep.quiz.ViolationType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ViolationRequest {
    @NotNull
    private ViolationType type;
}
