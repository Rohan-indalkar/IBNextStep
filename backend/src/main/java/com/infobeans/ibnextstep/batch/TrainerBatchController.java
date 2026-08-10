package com.infobeans.ibnextstep.batch;

import com.infobeans.ibnextstep.batch.dto.TrainerBatchSummaryResponse;
import com.infobeans.ibnextstep.batch.dto.TrainerDashboardStats;
import com.infobeans.ibnextstep.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Trainer's own view of the batches they're assigned to — separate from
 * BatchController (/api/admin/batches), which is ADMIN-only. Backs the
 * "Overview" screen's stat cards and "My batches" list.
 */
@RestController
@RequestMapping("/api/trainer/batches")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TRAINER')")
public class TrainerBatchController {

    private final TrainerBatchService trainerBatchService;

    @GetMapping("/mine")
    public ApiResponse<List<TrainerBatchSummaryResponse>> myBatches(Authentication authentication) {
        return ApiResponse.success(trainerBatchService.myBatches(authentication.getName()));
    }

    @GetMapping("/stats")
    public ApiResponse<TrainerDashboardStats> stats(Authentication authentication) {
        return ApiResponse.success(trainerBatchService.stats(authentication.getName()));
    }
}
