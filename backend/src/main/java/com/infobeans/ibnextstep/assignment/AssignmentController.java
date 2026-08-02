package com.infobeans.ibnextstep.assignment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infobeans.ibnextstep.assignment.dto.*;
import com.infobeans.ibnextstep.common.ApiResponse;
import com.infobeans.ibnextstep.common.PagedResponse;
import com.infobeans.ibnextstep.material.DifficultyLevel;
import com.infobeans.ibnextstep.user.TrainerType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Trainer's "Assignments" workspace: AI-generate or manually build a
 * practice assignment (typed questions and/or a reference PDF), publish it
 * to batches, review submissions, and give written feedback + a 1-5 rating.
 */
@RestController
@RequestMapping("/api/trainer/assignments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TRAINER')")
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final ObjectMapper objectMapper;

    /** "Create assignment through AI". */
    @PostMapping("/generate-ai")
    public ApiResponse<AssignmentResponse> generateWithAi(Authentication authentication, @Valid @RequestBody GenerateAssignmentRequest request) {
        return ApiResponse.success("Assignment generated — review and publish when ready", assignmentService.generateWithAi(authentication.getName(), request));
    }

    /**
     * Manual create. multipart/form-data with two parts:
     *  - "data"  : JSON body matching CreateAssignmentRequest, sent as a plain string part
     *              and parsed manually (Swagger UI/springdoc don't reliably tag JSON
     *              multipart parts with the right Content-Type otherwise).
     *  - "files" : zero or more reference files (e.g. a PDF of questions)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AssignmentResponse> create(
            Authentication authentication,
            @RequestPart("data") String dataJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) throws Exception {
        CreateAssignmentRequest data = objectMapper.readValue(dataJson, CreateAssignmentRequest.class);
        return ApiResponse.success("Assignment saved", assignmentService.create(authentication.getName(), data, files));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AssignmentResponse> update(
            Authentication authentication, @PathVariable String id,
            @RequestPart("data") String dataJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> newFiles) throws Exception {
        CreateAssignmentRequest data = objectMapper.readValue(dataJson, CreateAssignmentRequest.class);
        return ApiResponse.success("Assignment updated", assignmentService.update(authentication.getName(), id, data, newFiles));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Authentication authentication, @PathVariable String id) {
        assignmentService.delete(authentication.getName(), id);
        return ApiResponse.success("Assignment deleted", null);
    }

    @DeleteMapping("/{id}/files/{fileId}")
    public ApiResponse<AssignmentResponse> deleteFile(Authentication authentication, @PathVariable String id, @PathVariable String fileId) {
        return ApiResponse.success("File removed", assignmentService.deleteFile(authentication.getName(), id, fileId));
    }

    @PatchMapping("/{id}/publish")
    public ApiResponse<AssignmentResponse> publishNow(Authentication authentication, @PathVariable String id) {
        return ApiResponse.success("Assignment published", assignmentService.publishNow(authentication.getName(), id));
    }

    @PatchMapping("/{id}/schedule")
    public ApiResponse<AssignmentResponse> schedule(Authentication authentication, @PathVariable String id, @Valid @RequestBody ScheduleAssignmentRequest request) {
        return ApiResponse.success("Assignment scheduled", assignmentService.schedule(authentication.getName(), id, request.getScheduledAt()));
    }

    @PatchMapping("/{id}/close")
    public ApiResponse<AssignmentResponse> close(Authentication authentication, @PathVariable String id) {
        return ApiResponse.success("Assignment closed", assignmentService.close(authentication.getName(), id));
    }

    @GetMapping("/{id}")
    public ApiResponse<AssignmentResponse> getOne(Authentication authentication, @PathVariable String id) {
        return ApiResponse.success(assignmentService.getOne(authentication.getName(), id));
    }

    @GetMapping
    public ApiResponse<PagedResponse<AssignmentSummaryResponse>> dashboard(
            Authentication authentication,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) TrainerType skillType,
            @RequestParam(required = false) DifficultyLevel difficultyLevel,
            @RequestParam(required = false) AssignmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        AssignmentSearchCriteria criteria = AssignmentSearchCriteria.builder()
                .search(search).courseId(courseId).batchId(batchId).skillType(skillType)
                .difficultyLevel(difficultyLevel).status(status)
                .build();

        return ApiResponse.success(assignmentService.dashboard(authentication.getName(), criteria, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}/submissions")
    public ApiResponse<List<AssignmentSubmissionResponse>> submissions(Authentication authentication, @PathVariable String id) {
        return ApiResponse.success(assignmentService.submissions(authentication.getName(), id));
    }

    @GetMapping("/{id}/submissions/{studentId}")
    public ApiResponse<AssignmentSubmissionResponse> submissionDetail(Authentication authentication, @PathVariable String id, @PathVariable String studentId) {
        return ApiResponse.success(assignmentService.submissionDetail(authentication.getName(), id, studentId));
    }

    /** Practice feedback: written comments + 1-5 rating (no numeric score). */
    @PostMapping("/{id}/submissions/{studentId}/feedback")
    public ApiResponse<AssignmentSubmissionResponse> grade(
            Authentication authentication, @PathVariable String id, @PathVariable String studentId,
            @Valid @RequestBody GradeSubmissionRequest request) {
        return ApiResponse.success("Feedback shared with student", assignmentService.grade(authentication.getName(), id, studentId, request));
    }

    @GetMapping("/{id}/files/{fileId}/download")
    public ResponseEntity<Resource> downloadReferenceFile(Authentication authentication, @PathVariable String id, @PathVariable String fileId) {
        var meta = assignmentService.getReferenceFileMeta(authentication.getName(), id, fileId);
        Resource resource = assignmentService.downloadReferenceFile(authentication.getName(), id, fileId);
        return fileResponse(meta.getMimeType(), meta.getOriginalFileName(), resource);
    }

    @GetMapping("/{id}/submissions/{studentId}/files/{fileId}/download")
    public ResponseEntity<Resource> downloadSubmissionFile(
            Authentication authentication, @PathVariable String id, @PathVariable String studentId, @PathVariable String fileId) {
        var meta = assignmentService.getSubmissionFileMeta(authentication.getName(), id, studentId, fileId);
        Resource resource = assignmentService.downloadSubmissionFile(authentication.getName(), id, studentId, fileId);
        return fileResponse(meta.getMimeType(), meta.getOriginalFileName(), resource);
    }

    private ResponseEntity<Resource> fileResponse(String mimeType, String fileName, Resource resource) {
        MediaType mediaType = mimeType != null ? MediaType.parseMediaType(mimeType) : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }
}