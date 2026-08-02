package com.infobeans.ibnextstep.assignment.dto;

import com.infobeans.ibnextstep.assignment.AssignmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** One row of the trainer's Assignment dashboard. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentSummaryResponse {
    private String id;
    private String title;
    private String courseName;
    private AssignmentStatus status;
    private int questionCount;
    private boolean generatedByAI;
    private Instant dueDate;
    private Instant publishedAt;

    private long totalSubmissions;
    private long gradedSubmissions;
}
