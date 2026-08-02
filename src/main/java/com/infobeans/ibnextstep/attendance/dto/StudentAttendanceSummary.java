package com.infobeans.ibnextstep.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAttendanceSummary {
    private String studentId;
    private String firstName;
    private String lastName;

    private int totalDays;
    private int presentCount;
    private int absentCount;
    private int lateCount;

    /** presentCount / totalDays * 100, rounded to 2 decimals. */
    private double attendancePercentage;
}
