package com.infobeans.ibnextstep.placement;

import com.infobeans.ibnextstep.common.ApiResponse;
import com.infobeans.ibnextstep.common.PagedResponse;
import com.infobeans.ibnextstep.placement.dto.RejectOpportunityRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/approvals")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ApprovalController {

    private final ApprovalService approvalService;

    @GetMapping("/pending")
    public ApiResponse<PagedResponse<PlacementOpportunity>> pending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(approvalService.pendingApprovals(PageRequest.of(page, size)));
    }

    @PatchMapping("/{id}/approve")
    public ApiResponse<PlacementOpportunity> approve(@PathVariable String id) {
        return ApiResponse.success("Opportunity approved and published", approvalService.approve(id));
    }

    @PatchMapping("/{id}/reject")
    public ApiResponse<PlacementOpportunity> reject(@PathVariable String id, @Valid @RequestBody RejectOpportunityRequest request) {
        return ApiResponse.success("Opportunity rejected", approvalService.reject(id, request));
    }
}
