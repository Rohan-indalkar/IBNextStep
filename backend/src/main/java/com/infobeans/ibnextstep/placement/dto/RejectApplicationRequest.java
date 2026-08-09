package com.infobeans.ibnextstep.placement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectApplicationRequest {

    @NotBlank(message = "A rejection reason is required")
    private String reason;
}
