package com.infobeans.ibnextstep.studentevaluation;

import com.infobeans.ibnextstep.common.ApiResponse;
import com.infobeans.ibnextstep.studentevaluation.dto.BatchEvaluationOverviewResponse;
import com.infobeans.ibnextstep.studentevaluation.dto.CombinedEvaluationResponse;
import com.infobeans.ibnextstep.studentevaluation.dto.EvaluationMetricsResponse;
import com.infobeans.ibnextstep.studentevaluation.dto.EvaluationRubricResponse;
import com.infobeans.ibnextstep.studentevaluation.dto.StudentEvaluationResponse;
import com.infobeans.ibnextstep.studentevaluation.dto.SubmitEvaluationRequest;
import com.infobeans.ibnextstep.studentevaluation.dto.UpdateEvaluationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Trainer's "Student Evaluation" workspace. Typical flow:
 *  1. GET batch/{batchId} — pick a student from the roster with a quick eligibility snapshot
 *  2. GET rubric — which skills to score, based on the trainer's own type
 *  3. GET metrics/{studentId} — preview the system's auto-pulled numbers + eligibility verdict
 *  4. POST {studentId} — submit rubric scores + remarks; creates the permanent evaluation record
 *  5. PUT {id} — correct a mistake on an evaluation already submitted
 */
@RestController
@RequestMapping("/api/trainer/student-evaluations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TRAINER')")
public class TrainerStudentEvaluationController {

    private final StudentEvaluationService studentEvaluationService;
    private final EvaluationExportService evaluationExportService;

    @GetMapping("/rubric")
    public ApiResponse<EvaluationRubricResponse> getRubric(Authentication authentication) {
        return ApiResponse.success(studentEvaluationService.getRubric(authentication.getName()));
    }

    @GetMapping("/metrics/{studentId}")
    public ApiResponse<EvaluationMetricsResponse> getMetrics(Authentication authentication, @PathVariable String studentId) {
        return ApiResponse.success(studentEvaluationService.getMetrics(authentication.getName(), studentId));
    }

    /** "List my batch's students" — roster with a quick eligibility snapshot, so the trainer can pick who to evaluate. */
    @GetMapping("/batch/{batchId}")
    public ApiResponse<BatchEvaluationOverviewResponse> getBatchOverview(Authentication authentication, @PathVariable String batchId) {
        return ApiResponse.success(studentEvaluationService.getBatchOverview(authentication.getName(), batchId));
    }

    @PostMapping("/{studentId}")
    public ApiResponse<StudentEvaluationResponse> submit(
            Authentication authentication,
            @PathVariable String studentId,
            @Valid @RequestBody SubmitEvaluationRequest request) {
        return ApiResponse.success(
                "Evaluation submitted",
                studentEvaluationService.submit(authentication.getName(), studentId, request)
        );
    }

    /** Correct a mistake on an evaluation already submitted. Only the original trainer may edit it. */
    @PutMapping("/{id}")
    public ApiResponse<StudentEvaluationResponse> update(
            Authentication authentication,
            @PathVariable String id,
            @Valid @RequestBody UpdateEvaluationRequest request) {
        return ApiResponse.success(
                "Evaluation updated",
                studentEvaluationService.update(authentication.getName(), id, request)
        );
    }

    /** Combined Technical + Soft-Skill view for a student — latest of each rubric side by side. */
    @GetMapping("/combined/{studentId}")
    public ApiResponse<CombinedEvaluationResponse> getCombined(@PathVariable String studentId) {
        return ApiResponse.success(studentEvaluationService.getCombinedView(studentId));
    }

    @GetMapping("/student/{studentId}")
    public ApiResponse<List<StudentEvaluationResponse>> getHistory(@PathVariable String studentId) {
        return ApiResponse.success(studentEvaluationService.getHistoryForStudent(studentId));
    }

    @GetMapping("/{id}")
    public ApiResponse<StudentEvaluationResponse> getOne(@PathVariable String id) {
        return ApiResponse.success(studentEvaluationService.getOne(id));
    }

    /** PDF export of one evaluation report — the trainer's own submissions only. */
    @GetMapping("/{id}/export/pdf")
    public ResponseEntity<byte[]> exportPdf(Authentication authentication, @PathVariable String id) {
        StudentEvaluationResponse evaluation = studentEvaluationService.getOneOwnedByTrainer(authentication.getName(), id);
        byte[] pdfBytes = evaluationExportService.exportToPdf(evaluation);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=evaluation_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    /** Excel export of one evaluation report — the trainer's own submissions only. */
    @GetMapping("/{id}/export/excel")
    public ResponseEntity<byte[]> exportExcel(Authentication authentication, @PathVariable String id) {
        StudentEvaluationResponse evaluation = studentEvaluationService.getOneOwnedByTrainer(authentication.getName(), id);
        byte[] excelBytes = evaluationExportService.exportToExcel(evaluation);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=evaluation_" + id + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelBytes);
    }
}
