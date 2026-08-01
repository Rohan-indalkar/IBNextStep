package com.infobeans.ibnextstep.quiz;

import com.infobeans.ibnextstep.common.ApiResponse;
import com.infobeans.ibnextstep.quiz.dto.AutoSaveRequest;
import com.infobeans.ibnextstep.quiz.dto.StudentQuestionView;
import com.infobeans.ibnextstep.quiz.dto.SubmitQuizRequest;
import com.infobeans.ibnextstep.quiz.dto.ViolationRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student/quizzes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentQuizController {

    private final StudentQuizService studentQuizService;

    @GetMapping
    public ApiResponse<List<Quiz>> listAssigned(Authentication auth) {
        return ApiResponse.success(studentQuizService.listAssigned(auth.getName()));
    }

    @GetMapping("/{id}")
    public ApiResponse<Quiz> getInstructions(@PathVariable String id, Authentication auth) {
        return ApiResponse.success(studentQuizService.getInstructions(auth.getName(), id));
    }

    @PostMapping("/{id}/start")
    public ApiResponse<List<StudentQuestionView>> start(@PathVariable String id, Authentication auth, HttpServletRequest httpRequest) {
        String ip = extractIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ApiResponse.success(studentQuizService.start(auth.getName(), id, ip, userAgent));
    }

    @PostMapping("/{id}/autosave")
    public ApiResponse<Void> autoSave(@PathVariable String id, @Valid @RequestBody AutoSaveRequest request, Authentication auth) {
        studentQuizService.autoSave(auth.getName(), id, request);
        return ApiResponse.success("Progress saved", null);
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<QuizResult> submit(@PathVariable String id, @RequestBody(required = false) SubmitQuizRequest request, Authentication auth) {
        return ApiResponse.success("Quiz submitted", studentQuizService.submit(auth.getName(), id, request));
    }

    @PostMapping("/{id}/violation")
    public ApiResponse<Map<String, Object>> violation(@PathVariable String id, @Valid @RequestBody ViolationRequest request, Authentication auth) {
        return ApiResponse.success(studentQuizService.recordViolation(auth.getName(), id, request.getType()));
    }

    /** "IP Address Logging" — X-Forwarded-For first (behind a proxy/load balancer), falling back to the direct remote address. */
    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
