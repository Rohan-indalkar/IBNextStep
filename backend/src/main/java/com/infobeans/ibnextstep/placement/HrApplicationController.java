package com.infobeans.ibnextstep.placement;

import com.infobeans.ibnextstep.common.ApiResponse;
import com.infobeans.ibnextstep.common.PagedResponse;
import com.infobeans.ibnextstep.common.exception.ResourceNotFoundException;
import com.infobeans.ibnextstep.placement.dto.PlacementApplicationResponse;
import com.infobeans.ibnextstep.placement.dto.RejectApplicationRequest;
import com.infobeans.ibnextstep.placement.dto.RescheduleInterviewRequest;
import com.infobeans.ibnextstep.placement.dto.ScheduleInterviewRequest;
import com.infobeans.ibnextstep.placement.dto.UpdateInterviewResultRequest;
import com.infobeans.ibnextstep.user.Role;
import com.infobeans.ibnextstep.user.User;
import com.infobeans.ibnextstep.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * HR's recruitment pipeline: filter applications, shortlist/reject
 * candidates, manage interview rounds, and record the final decision.
 */
@RestController
@RequestMapping("/api/hr/applications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('HR')")
public class HrApplicationController {

    private final PlacementApplicationService applicationService;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<PagedResponse<PlacementApplicationResponse>> search(
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String placementId,
            @RequestParam(required = false) PlacementApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PlacementApplicationSearchCriteria criteria = PlacementApplicationSearchCriteria.builder()
                .companyId(companyId)
                .departmentId(departmentId)
                .placementId(placementId)
                .status(status)
                .build();
        return ApiResponse.success(applicationService.search(criteria, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<PlacementApplicationResponse> getById(@PathVariable String id) {
        return ApiResponse.success(applicationService.getOne(id));
    }

    @PatchMapping("/{id}/shortlist")
    public ApiResponse<PlacementApplicationResponse> shortlist(Authentication authentication, @PathVariable String id) {
        return ApiResponse.success("Candidate shortlisted", applicationService.shortlist(currentHr(authentication), id));
    }

    @PatchMapping("/{id}/reject")
    public ApiResponse<PlacementApplicationResponse> reject(Authentication authentication, @PathVariable String id,
                                                              @Valid @RequestBody RejectApplicationRequest request) {
        return ApiResponse.success("Candidate rejected", applicationService.reject(currentHr(authentication), id, request));
    }

    @PatchMapping("/{id}/select")
    public ApiResponse<PlacementApplicationResponse> select(Authentication authentication, @PathVariable String id) {
        return ApiResponse.success("Candidate selected", applicationService.selectCandidate(currentHr(authentication), id));
    }

    @PatchMapping("/{id}/not-select")
    public ApiResponse<PlacementApplicationResponse> notSelect(Authentication authentication, @PathVariable String id,
                                                                 @Valid @RequestBody RejectApplicationRequest request) {
        return ApiResponse.success("Candidate marked not selected", applicationService.markNotSelected(currentHr(authentication), id, request));
    }

    // ---------- Interview rounds ----------

    @PostMapping("/{id}/interview-rounds")
    public ApiResponse<PlacementApplicationResponse> scheduleRound(Authentication authentication, @PathVariable String id,
                                                                     @Valid @RequestBody ScheduleInterviewRequest request) {
        return ApiResponse.success("Interview round scheduled", applicationService.scheduleRound(currentHr(authentication), id, request));
    }

    @PutMapping("/{id}/interview-rounds/{roundNumber}/reschedule")
    public ApiResponse<PlacementApplicationResponse> rescheduleRound(Authentication authentication, @PathVariable String id,
                                                                       @PathVariable int roundNumber,
                                                                       @Valid @RequestBody RescheduleInterviewRequest request) {
        return ApiResponse.success("Interview round rescheduled",
                applicationService.rescheduleRound(currentHr(authentication), id, roundNumber, request));
    }

    @PatchMapping("/{id}/interview-rounds/{roundNumber}/cancel")
    public ApiResponse<PlacementApplicationResponse> cancelRound(Authentication authentication, @PathVariable String id,
                                                                    @PathVariable int roundNumber,
                                                                    @Valid @RequestBody RejectApplicationRequest request) {
        return ApiResponse.success("Interview round cancelled",
                applicationService.cancelRound(currentHr(authentication), id, roundNumber, request));
    }

    @PatchMapping("/{id}/interview-rounds/{roundNumber}/result")
    public ApiResponse<PlacementApplicationResponse> updateRoundResult(Authentication authentication, @PathVariable String id,
                                                                         @PathVariable int roundNumber,
                                                                         @Valid @RequestBody UpdateInterviewResultRequest request) {
        return ApiResponse.success("Interview round result recorded",
                applicationService.updateRoundResult(currentHr(authentication), id, roundNumber, request));
    }

    private User currentHr(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.HR) {
            throw new ResourceNotFoundException("User not found");
        }
        return user;
    }
}
