package com.infobeans.ibnextstep.mockinterview.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RescheduleMockInterviewRequest {

    @NotNull(message = "Scheduled date & time is required")
    @Future(message = "Scheduled date & time must be in the future")
    private Instant scheduledAt;

    private Integer durationMinutes;
    private String meetingLink;
    private String notes;
}
