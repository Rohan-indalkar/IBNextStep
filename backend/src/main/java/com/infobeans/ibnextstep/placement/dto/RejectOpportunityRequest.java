package com.infobeans.ibnextstep.placement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectOpportunityRequest {
    @NotBlank
    private String reason;
}
