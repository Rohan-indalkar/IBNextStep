package com.infobeans.ibnextstep.mockinterview.dto;

import com.infobeans.ibnextstep.mockinterview.InterviewType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMockInterviewRequest {

    @NotBlank(message = "Batch is required")
    private String batchId;

    @NotEmpty(message = "Select at least one student")
    private List<String> studentIds;

    @NotNull(message = "Interview type is required")
    private InterviewType interviewType;

    @NotNull(message = "Scheduled date & time is required")
    @Future(message = "Scheduled date & time must be in the future")
    private Instant scheduledAt;

    private Integer durationMinutes;

    /** Optional — leave blank to auto-generate a meeting room link. */
    private String meetingLink;

    /** Included in the invite email/notification sent to each student. */
    private String notes;
}
