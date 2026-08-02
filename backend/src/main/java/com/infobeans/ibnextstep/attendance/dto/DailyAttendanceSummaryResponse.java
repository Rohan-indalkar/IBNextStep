package com.infobeans.ibnextstep.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** "View daily attendance summary" for a batch. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyAttendanceSummaryResponse {
    private String batchId;
    private LocalDate date;
    private int totalStudents;
    private int presentCount;
    private int absentCount;
    private int lateCount;
    private int notMarkedCount;
}
