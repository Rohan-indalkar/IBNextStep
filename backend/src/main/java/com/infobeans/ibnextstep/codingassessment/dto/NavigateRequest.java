package com.infobeans.ibnextstep.codingassessment.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class NavigateRequest {
    @Min(0)
    private int questionIndex;
}
