package com.infobeans.ibnextstep.codingassessment;

import com.infobeans.ibnextstep.codingassessment.dto.*;
import com.infobeans.ibnextstep.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student/assessments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentAssessmentController {

    private final StudentAssessmentService assessmentService;

    @GetMapping
    public ApiResponse<List<Assessment>> listAssigned(Authentication auth) {
        return ApiResponse.success(assessmentService.listAssigned(auth.getName()));
    }

    @PostMapping("/{id}/start")
    public ApiResponse<AssessmentSession> start(@PathVariable String id, Authentication auth, HttpServletRequest httpRequest) {
        return ApiResponse.success(assessmentService.start(auth.getName(), id,
                extractIp(httpRequest), parseBrowser(httpRequest), parseOs(httpRequest)));
    }

    @GetMapping("/{id}/questions/{questionId}")
    public ApiResponse<QuestionStudentView> getQuestion(@PathVariable String id, @PathVariable String questionId, Authentication auth) {
        return ApiResponse.success(assessmentService.getQuestion(auth.getName(), id, questionId));
    }

    @PostMapping("/{id}/navigate")
    public ApiResponse<AssessmentSession> navigate(@PathVariable String id, @Valid @RequestBody NavigateRequest request, Authentication auth) {
        return ApiResponse.success(assessmentService.navigate(auth.getName(), id, request));
    }

    @PostMapping("/{id}/questions/{questionId}/draft")
    public ApiResponse<Void> saveDraft(@PathVariable String id, @PathVariable String questionId,
                                        @Valid @RequestBody SaveDraftRequest request, Authentication auth) {
        assessmentService.saveDraft(auth.getName(), id, questionId, request);
        return ApiResponse.success("Draft saved", null);
    }

    @PostMapping("/{id}/questions/{questionId}/run")
    public ApiResponse<RunCodeResponse> runCode(@PathVariable String id, @PathVariable String questionId,
                                                  @Valid @RequestBody RunCodeRequest request, Authentication auth) {
        return ApiResponse.success(assessmentService.runCode(auth.getName(), id, questionId, request));
    }

    @PostMapping("/{id}/questions/{questionId}/submit")
    public ApiResponse<Submission> submitQuestion(@PathVariable String id, @PathVariable String questionId,
                                                    @Valid @RequestBody SubmitCodeRequest request, Authentication auth) {
        return ApiResponse.success("Submitted", assessmentService.submitQuestion(auth.getName(), id, questionId, request));
    }

    @GetMapping("/{id}/review")
    public ApiResponse<List<Submission>> review(@PathVariable String id, Authentication auth) {
        return ApiResponse.success(assessmentService.reviewSubmissions(auth.getName(), id));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<AssessmentSession> complete(@PathVariable String id, Authentication auth) {
        return ApiResponse.success("Assessment completed", assessmentService.completeAssessment(auth.getName(), id));
    }

    @PostMapping("/{id}/warning")
    public ApiResponse<Map<String, Object>> warning(@PathVariable String id, @Valid @RequestBody WarningEventRequest request, Authentication auth) {
        return ApiResponse.success(assessmentService.recordWarning(auth.getName(), id, request.getType()));
    }

    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String parseBrowser(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return ua == null ? "Unknown" : ua;
    }

    private String parseOs(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        if (ua == null) return "Unknown";
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Mac OS")) return "macOS";
        if (ua.contains("Linux")) return "Linux";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("iOS") || ua.contains("iPhone")) return "iOS";
        return "Unknown";
    }
}
