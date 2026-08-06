package com.infobeans.ibnextstep.placement;

import com.infobeans.ibnextstep.common.ApiResponse;
import com.infobeans.ibnextstep.common.exception.ResourceNotFoundException;
import com.infobeans.ibnextstep.placement.dto.StudentPlacementDashboardResponse;
import com.infobeans.ibnextstep.user.Role;
import com.infobeans.ibnextstep.user.User;
import com.infobeans.ibnextstep.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/dashboard/placements")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentPlacementDashboardController {

    private final PlacementDashboardService dashboardService;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<StudentPlacementDashboardResponse> getDashboard(Authentication authentication) {
        User student = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (student.getRole() != Role.STUDENT) {
            throw new ResourceNotFoundException("User not found");
        }
        return ApiResponse.success(dashboardService.getStudentDashboard(student.getId()));
    }
}
