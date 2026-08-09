package com.infobeans.ibnextstep.studentevaluation;

import com.infobeans.ibnextstep.common.ApiResponse;
import com.infobeans.ibnextstep.common.exception.ResourceNotFoundException;
import com.infobeans.ibnextstep.studentevaluation.dto.CombinedEvaluationResponse;
import com.infobeans.ibnextstep.studentevaluation.dto.StudentEvaluationResponse;
import com.infobeans.ibnextstep.user.Role;
import com.infobeans.ibnextstep.user.User;
import com.infobeans.ibnextstep.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Student's own read-only view of their evaluation history —
 * scores, remarks, and the eligibility verdict trainers have recorded for them.
 */
@RestController
@RequestMapping("/api/student/evaluations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentEvaluationController {

    private final StudentEvaluationService studentEvaluationService;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<List<StudentEvaluationResponse>> getMyEvaluations(Authentication authentication) {
        User student = currentStudent(authentication);
        return ApiResponse.success(studentEvaluationService.getHistoryForStudent(student.getId()));
    }

    /** Combined Technical + Soft-Skill view — the student's own latest evaluation from each rubric, side by side. */
    @GetMapping("/combined")
    public ApiResponse<CombinedEvaluationResponse> getMyCombinedEvaluation(Authentication authentication) {
        User student = currentStudent(authentication);
        return ApiResponse.success(studentEvaluationService.getCombinedView(student.getId()));
    }

    private User currentStudent(Authentication authentication) {
        User student = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (student.getRole() != Role.STUDENT) {
            throw new ResourceNotFoundException("User not found");
        }
        return student;
    }
}
