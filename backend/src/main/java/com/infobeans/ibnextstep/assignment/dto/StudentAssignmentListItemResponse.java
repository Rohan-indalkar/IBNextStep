package com.infobeans.ibnextstep.assignment.dto;

import com.infobeans.ibnextstep.assignment.AssignmentSubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** One row of the student's assignment list. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAssignmentListItemResponse {
    private String assignmentId;
    private String title;
    private String courseName;
    private String topic;
    private int questionCount;
    private boolean hasReferenceFiles;
    private Instant dueDate;

    /** null if the student hasn't submitted yet. */
    private AssignmentSubmissionStatus submissionStatus;
    private Integer rating;
}
