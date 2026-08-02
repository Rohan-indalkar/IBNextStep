package com.infobeans.ibnextstep.assignment.dto;

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
public class StudentAssignmentDetailResponse {
    private String assignmentId;
    private String title;
    private String description;
    private String courseName;
    private String topic;
    private List<AssignmentQuestionResponse> questions;
    private List<AssignmentFileResponse> referenceFiles;
    private Instant dueDate;

    /** My own submission, if I've made one. */
    private AssignmentSubmissionResponse mySubmission;
}
