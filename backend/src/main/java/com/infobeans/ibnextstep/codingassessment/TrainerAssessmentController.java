package com.infobeans.ibnextstep.codingassessment;

import com.infobeans.ibnextstep.codingassessment.dto.*;
import com.infobeans.ibnextstep.common.ApiResponse;
import com.infobeans.ibnextstep.common.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trainer/assessments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TRAINER')")
public class TrainerAssessmentController {

    private final TrainerAssessmentService assessmentService;
    private final AssessmentAnalyticsService analyticsService;

    // ---------- Assessment lifecycle ----------

    @PostMapping
    public ApiResponse<Assessment> create(@Valid @RequestBody CreateAssessmentRequest request, Authentication auth) {
        return ApiResponse.success("Assessment created as DRAFT", assessmentService.create(auth.getName(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Assessment> update(@PathVariable String id, @Valid @RequestBody UpdateAssessmentRequest request, Authentication auth) {
        return ApiResponse.success("Assessment updated", assessmentService.update(auth.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id, Authentication auth) {
        assessmentService.delete(auth.getName(), id);
        return ApiResponse.success("Assessment deleted", null);
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<Assessment> publish(@PathVariable String id, Authentication auth) {
        return ApiResponse.success("Assessment published and students notified", assessmentService.publish(auth.getName(), id));
    }

    @PostMapping("/{id}/archive")
    public ApiResponse<Assessment> archive(@PathVariable String id, Authentication auth) {
        return ApiResponse.success("Assessment archived", assessmentService.archive(auth.getName(), id));
    }

    @PostMapping("/{id}/duplicate")
    public ApiResponse<Assessment> duplicate(@PathVariable String id, Authentication auth) {
        return ApiResponse.success("Assessment duplicated as DRAFT", assessmentService.duplicate(auth.getName(), id));
    }

    @GetMapping
    public ApiResponse<PagedResponse<Assessment>> list(@RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size, Authentication auth) {
        return ApiResponse.success(PagedResponse.from(assessmentService.list(auth.getName(), PageRequest.of(page, size))));
    }

    @GetMapping("/{id}")
    public ApiResponse<Assessment> getOne(@PathVariable String id, Authentication auth) {
        return ApiResponse.success(assessmentService.getOne(auth.getName(), id));
    }

    // ---------- Questions ----------

    @PostMapping("/{id}/questions")
    public ApiResponse<CodingQuestion> addQuestion(@PathVariable String id, @Valid @RequestBody CreateQuestionRequest request, Authentication auth) {
        return ApiResponse.success("Question added", assessmentService.addQuestion(auth.getName(), id, request));
    }

    @PutMapping("/{id}/questions/{questionId}")
    public ApiResponse<CodingQuestion> editQuestion(@PathVariable String id, @PathVariable String questionId,
                                                      @Valid @RequestBody CreateQuestionRequest request, Authentication auth) {
        return ApiResponse.success("Question updated", assessmentService.editQuestion(auth.getName(), id, questionId, request));
    }

    @DeleteMapping("/{id}/questions/{questionId}")
    public ApiResponse<Void> deleteQuestion(@PathVariable String id, @PathVariable String questionId, Authentication auth) {
        assessmentService.deleteQuestion(auth.getName(), id, questionId);
        return ApiResponse.success("Question deleted", null);
    }

    @GetMapping("/{id}/questions")
    public ApiResponse<List<CodingQuestion>> listQuestions(@PathVariable String id, Authentication auth) {
        return ApiResponse.success(assessmentService.listQuestions(auth.getName(), id));
    }

    @GetMapping("/questions/{questionId}/test-cases")
    public ApiResponse<List<TestCase>> listTestCases(@PathVariable String questionId, Authentication auth) {
        return ApiResponse.success(assessmentService.listAllTestCases(auth.getName(), questionId));
    }

    // ---------- AI Generation ----------

    /** Preview only — nothing is saved until the trainer reviews and calls /questions/ai (below). */
    @PostMapping("/questions/generate")
    public ApiResponse<List<CreateQuestionRequest>> generatePreview(@Valid @RequestBody GenerateQuestionsRequest request, Authentication auth) {
        return ApiResponse.success("AI-generated questions — review before saving", assessmentService.generateQuestionsPreview(auth.getName(), request));
    }

    @PostMapping("/{id}/questions/ai")
    public ApiResponse<List<CodingQuestion>> saveAiQuestions(@PathVariable String id, @Valid @RequestBody List<CreateQuestionRequest> requests, Authentication auth) {
        return ApiResponse.success("AI-generated questions saved", assessmentService.addAiGeneratedQuestions(auth.getName(), id, requests));
    }

    @PostMapping("/{id}/questions/{questionId}/regenerate")
    public ApiResponse<CodingQuestion> regenerateQuestion(@PathVariable String id, @PathVariable String questionId,
                                                            @Valid @RequestBody GenerateQuestionsRequest request, Authentication auth) {
        return ApiResponse.success("Question regenerated", assessmentService.regenerateQuestion(auth.getName(), id, questionId, request));
    }

    // ---------- Results / Analytics ----------

    @GetMapping("/{id}/submissions")
    public ApiResponse<List<Submission>> submissions(@PathVariable String id) {
        return ApiResponse.success(analyticsService.allSubmissions(id));
    }

    @GetMapping("/{id}/sessions")
    public ApiResponse<List<AssessmentSession>> sessions(@PathVariable String id) {
        return ApiResponse.success(analyticsService.allSessions(id));
    }

    @GetMapping("/{id}/analytics")
    public ApiResponse<TrainerAnalyticsResponse> analytics(@PathVariable String id) {
        return ApiResponse.success(analyticsService.analyze(id));
    }
}
