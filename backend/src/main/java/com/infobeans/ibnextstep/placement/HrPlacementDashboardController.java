package com.infobeans.ibnextstep.placement;

import com.infobeans.ibnextstep.common.ApiResponse;
import com.infobeans.ibnextstep.placement.dto.HrDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hr/dashboard/placements")
@RequiredArgsConstructor
@PreAuthorize("hasRole('HR')")
public class HrPlacementDashboardController {

    private final PlacementDashboardService dashboardService;

    @GetMapping
    public ApiResponse<HrDashboardResponse> getDashboard() {
        return ApiResponse.success(dashboardService.getHrDashboard());
    }
}
