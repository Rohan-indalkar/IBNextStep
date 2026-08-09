package com.infobeans.ibnextstep.mockinterview;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockInterviewSearchCriteria {
    private String trainerId;
    private String batchId;
    private String studentId;
    private InterviewType interviewType;
    private MockInterviewStatus status;
    private Instant scheduledFrom;
    private Instant scheduledTo;
}
