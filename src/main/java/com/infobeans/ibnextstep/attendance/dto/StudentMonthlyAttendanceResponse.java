package com.infobeans.ibnextstep.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** "View monthly attendance" + "View attendance percentage", combined for the student's screen. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentMonthlyAttendanceResponse {
    private int month;
    private int year;

    private int totalDays;
    private int presentCount;
    private int absentCount;
    private int lateCount;
    private double attendancePercentage;

    private List<AttendanceRecordResponse> records;
}
