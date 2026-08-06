package com.infobeans.ibnextstep.placement.dto;

import com.infobeans.ibnextstep.placement.InterviewRoundResult;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateInterviewResultRequest {

    @NotNull(message = "Result is required")
    private InterviewRoundResult result;

    private String resultRemarks;
}
