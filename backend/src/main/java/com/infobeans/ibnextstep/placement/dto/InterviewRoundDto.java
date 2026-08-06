package com.infobeans.ibnextstep.placement.dto;

import com.infobeans.ibnextstep.placement.InterviewRoundResult;
import com.infobeans.ibnextstep.placement.InterviewRoundStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewRoundDto {
    private int roundNumber;
    private String roundType;
    private Instant scheduledAt;
    private Integer durationMinutes;
    private String venue;
    private String meetingLink;
    private String remarks;
    private InterviewRoundStatus status;
    private InterviewRoundResult result;
    private String resultRemarks;
    private Instant updatedAt;
}
