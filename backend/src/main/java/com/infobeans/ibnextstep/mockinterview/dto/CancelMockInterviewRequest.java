package com.infobeans.ibnextstep.mockinterview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelMockInterviewRequest {

    @NotBlank(message = "A cancellation reason is required")
    private String reason;
}
