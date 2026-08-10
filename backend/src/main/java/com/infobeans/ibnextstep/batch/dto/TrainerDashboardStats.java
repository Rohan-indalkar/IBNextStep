package com.infobeans.ibnextstep.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Backs the three stat cards on the trainer Overview screen
 * ("My batches" / "Active batches" / "Total students").
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainerDashboardStats {
    private long myBatches;
    private long activeBatches;
    private long totalStudents;
}
