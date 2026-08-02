package com.infobeans.ibnextstep.assignment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleAssignmentRequest {

    @NotNull(message = "scheduledAt is required")
    private Instant scheduledAt;
}
