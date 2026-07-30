package com.infobeans.ibnextstep.resume;

import com.infobeans.ibnextstep.common.ApiResponse;
import com.infobeans.ibnextstep.common.util.FileStorageService;
import com.infobeans.ibnextstep.resume.dto.ReviewResumeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trainer")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TRAINER')")
public class ResumeController {

    private final ResumeService resumeService;
    private final FileStorageService fileStorageService;

    /** "Select Student" list view — every resume across this trainer's batches, optionally filtered by status (e.g. PENDING_REVIEW). */
    @GetMapping("/resumes")
    public ApiResponse<List<Resume>> list(@RequestParam(required = false) ResumeStatus status, Authentication authentication) {
        return ApiResponse.success(resumeService.listForReview(authentication.getName(), status));
    }

    /** "View Student Resume" */
    @GetMapping("/students/{studentId}/resume")
    public ApiResponse<Resume> getStudentResume(@PathVariable String studentId, Authentication authentication) {
        return ApiResponse.success(resumeService.getStudentResume(authentication.getName(), studentId));
    }

    /** Streams the actual resume file for the trainer to open/inspect before reviewing. */
    @GetMapping("/students/{studentId}/resume/file")
    public ResponseEntity<Resource> downloadStudentResumeFile(@PathVariable String studentId, Authentication authentication) {
        Resume resume = resumeService.getStudentResume(authentication.getName(), studentId);
        Resume.ResumeVersion latest = resume.getVersions().get(resume.getVersions().size() - 1);
        Resource file = fileStorageService.loadAsResource(latest.getFilePath());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + latest.getFileName() + "\"")
                .body(file);
    }

    /** "Review Resume" -> "Provide Suggestions" -> "Assign Resume Score" -> "Choose Status" -> notifies the student automatically. */
    @PostMapping("/students/{studentId}/resume/review")
    public ApiResponse<Resume> review(@PathVariable String studentId, @Valid @RequestBody ReviewResumeRequest request, Authentication authentication) {
        return ApiResponse.success("Review submitted", resumeService.review(authentication.getName(), studentId, request));
    }
}
