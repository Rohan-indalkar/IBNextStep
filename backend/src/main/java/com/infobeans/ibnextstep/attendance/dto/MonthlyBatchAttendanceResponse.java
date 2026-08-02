package com.infobeans.ibnextstep.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** "View monthly attendance summary" for a batch — one row per student. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyBatchAttendanceResponse {
    private String batchId;
    private int month;
    private int year;
    private List<StudentAttendanceSummary> students;
}
