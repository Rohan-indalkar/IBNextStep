package com.infobeans.ibnextstep.studentevaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchEvaluationOverviewResponse {
    private String batchId;
    private String batchName;
    private int totalStudents;
    private int evaluatedByMe;
    private int pendingEvaluationByMe;
    private List<StudentEligibilitySnapshot> students;
}
