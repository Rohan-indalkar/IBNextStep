package com.infobeans.ibnextstep.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Internal — feeds {@code MonthlyAttendanceNotificationScheduler}, not exposed via any controller. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentMonthlyPercentage {
    private String studentId;
    private int totalDays;
    private int presentCount;
    private double attendancePercentage;
}
