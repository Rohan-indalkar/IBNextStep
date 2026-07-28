package com.infobeans.ibnextstep.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminDashboardStats {
    private long totalStudents;
    private long totalTrainers;
    private long totalHr;
    private long activeBatches;
    private long totalCourses;
    private long pendingApprovals;
}
