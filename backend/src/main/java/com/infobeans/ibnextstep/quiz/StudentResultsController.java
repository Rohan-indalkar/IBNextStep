package com.infobeans.ibnextstep.quiz;

import com.infobeans.ibnextstep.common.ApiResponse;
import com.infobeans.ibnextstep.common.exception.BadRequestException;
import com.infobeans.ibnextstep.common.exception.ResourceNotFoundException;
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

/** "Quiz History" — every quiz result this student has ever received, across all quizzes. */
@RestController
@RequestMapping("/api/student/results")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentResultsController {

    private final QuizResultRepository resultRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<List<QuizResult>> myResults(Authentication auth) {
        User student = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Student account not found"));
        if (student.getRole() != Role.STUDENT) {
            throw new BadRequestException("Only students can view this");
        }
        return ApiResponse.success(resultRepository.findByStudentId(student.getId()));
    }
}
