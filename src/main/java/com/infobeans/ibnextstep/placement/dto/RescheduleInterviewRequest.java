package com.infobeans.ibnextstep.placement.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class RescheduleInterviewRequest {

    @NotNull(message = "Scheduled date/time is required")
    private Instant scheduledAt;

    private Integer durationMinutes;
    private String venue;
    private String meetingLink;
    private String remarks;
}
