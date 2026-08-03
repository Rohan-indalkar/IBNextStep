package com.infobeans.ibnextstep.codingassessment.dto;

import com.infobeans.ibnextstep.codingassessment.WarningType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WarningEventRequest {
    @NotNull
    private WarningType type;
}
