package com.infobeans.ibnextstep.mockinterview;

import com.infobeans.ibnextstep.common.ApiResponse;
import com.infobeans.ibnextstep.common.PagedResponse;
import com.infobeans.ibnextstep.mockinterview.dto.CancelMockInterviewRequest;
import com.infobeans.ibnextstep.mockinterview.dto.CreateMockInterviewRequest;
import com.infobeans.ibnextstep.mockinterview.dto.MockInterviewAnalyticsResponse;
import com.infobeans.ibnextstep.mockinterview.dto.MockInterviewEvaluationRequest;
import com.infobeans.ibnextstep.mockinterview.dto.MockInterviewResponse;
import com.infobeans.ibnextstep.mockinterview.dto.RescheduleMockInterviewRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * Trainer's "Mock Interviews" workspace: schedule -> conduct -> evaluate ->
 * publish. All endpoints operate on interviews the calling trainer scheduled;
 * ownership is enforced in MockInterviewService.
 */
@RestController
@RequestMapping("/api/trainer/mock-interviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TRAINER')")
public class MockInterviewController {

    private final MockInterviewService mockInterviewService;

    /** Create Mock Interview -> Select Batch -> Select Student(s) -> Type -> Schedule -> Meeting Link -> notify. */
    @PostMapping
    public ApiResponse<List<MockInterviewResponse>> create(
            Authentication authentication, @Valid @RequestBody CreateMockInterviewRequest request) {
        return ApiResponse.success("Mock interview(s) scheduled", mockInterviewService.create(authentication.getName(), request));
    }

    @PutMapping("/{id}/reschedule")
    public ApiResponse<MockInterviewResponse> reschedule(
            Authentication authentication, @PathVariable String id, @Valid @RequestBody RescheduleMockInterviewRequest request) {
        return ApiResponse.success("Mock interview rescheduled", mockInterviewService.reschedule(authentication.getName(), id, request));
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<MockInterviewResponse> cancel(
            Authentication authentication, @PathVariable String id, @Valid @RequestBody CancelMockInterviewRequest request) {
        return ApiResponse.success("Mock interview cancelled", mockInterviewService.cancel(authentication.getName(), id, request));
    }

    /** Trainer Conducts Interview — click once the session is done. */
    @PatchMapping("/{id}/conducted")
    public ApiResponse<MockInterviewResponse> markConducted(Authentication authentication, @PathVariable String id) {
        return ApiResponse.success("Marked as conducted", mockInterviewService.markConducted(authentication.getName(), id));
    }

    /** Complete Evaluation Form -> Assign Scores -> Strengths/Weaknesses -> Suggestions -> Overall Rating. */
    @PostMapping("/{id}/evaluation")
    public ApiResponse<MockInterviewResponse> submitEvaluation(
            Authentication authentication, @PathVariable String id, @Valid @RequestBody MockInterviewEvaluationRequest request) {
        return ApiResponse.success("Evaluation saved", mockInterviewService.submitEvaluation(authentication.getName(), id, request));
    }

    /** Publish Evaluation Report -> updates student's dashboard + placement readiness score. */
    @PatchMapping("/{id}/publish")
    public ApiResponse<MockInterviewResponse> publish(Authentication authentication, @PathVariable String id) {
        return ApiResponse.success("Evaluation report published", mockInterviewService.publish(authentication.getName(), id));
    }

    @GetMapping("/{id}")
    public ApiResponse<MockInterviewResponse> getOne(Authentication authentication, @PathVariable String id) {
        return ApiResponse.success(mockInterviewService.getOne(authentication.getName(), id));
    }

    @GetMapping
    public ApiResponse<PagedResponse<MockInterviewResponse>> search(
            Authentication authentication,
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) InterviewType interviewType,
            @RequestParam(required = false) MockInterviewStatus status,
            @RequestParam(required = false) Instant scheduledFrom,
            @RequestParam(required = false) Instant scheduledTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        MockInterviewSearchCriteria criteria = MockInterviewSearchCriteria.builder()
                .batchId(batchId).studentId(studentId).interviewType(interviewType).status(status)
                .scheduledFrom(scheduledFrom).scheduledTo(scheduledTo)
                .build();

        return ApiResponse.success(mockInterviewService.search(authentication.getName(), criteria, PageRequest.of(page, size)));
    }

    /** Generate Analytics & Reports — optionally scoped to a batch and/or a single student. */
    @GetMapping("/analytics")
    public ApiResponse<MockInterviewAnalyticsResponse> analytics(
            Authentication authentication,
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) String studentId) {
        return ApiResponse.success(mockInterviewService.analytics(authentication.getName(), batchId, studentId));
    }
}
