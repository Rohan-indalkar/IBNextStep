package com.infobeans.ibnextstep.assignment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infobeans.ibnextstep.assignment.dto.AssignmentSubmissionResponse;
import com.infobeans.ibnextstep.assignment.dto.StudentAssignmentDetailResponse;
import com.infobeans.ibnextstep.assignment.dto.StudentAssignmentListItemResponse;
import com.infobeans.ibnextstep.assignment.dto.SubmitAssignmentRequest;
import com.infobeans.ibnextstep.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Student's "Assignments" view: browse what's assigned to their batch, open
 * one to read the question(s)/reference PDF, and submit an answer (text
 * and/or an uploaded file).
 */
@RestController
@RequestMapping("/api/student/assignments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentAssignmentController {

    private final StudentAssignmentService studentAssignmentService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ApiResponse<List<StudentAssignmentListItemResponse>> listAvailable(Authentication authentication) {
        return ApiResponse.success(studentAssignmentService.listAvailable(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ApiResponse<StudentAssignmentDetailResponse> getDetail(Authentication authentication, @PathVariable String id) {
        return ApiResponse.success(studentAssignmentService.getDetail(authentication.getName(), id));
    }

    /**
     * Submit — multipart/form-data with two parts:
     *  - "data"  : JSON body matching SubmitAssignmentRequest, sent as a plain string part
     *              and parsed manually (Swagger UI/springdoc don't reliably tag JSON
     *              multipart parts with the right Content-Type otherwise).
     *  - "files" : zero or more files (your solution, screenshots, code, etc.)
     */
    @PostMapping(value = "/{id}/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AssignmentSubmissionResponse> submit(
            Authentication authentication, @PathVariable String id,
            @RequestPart("data") String dataJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) throws Exception {
        SubmitAssignmentRequest data = objectMapper.readValue(dataJson, SubmitAssignmentRequest.class);
        return ApiResponse.success("Submitted", studentAssignmentService.submit(authentication.getName(), id, data, files));
    }

    @GetMapping("/{id}/files/{fileId}/download")
    public ResponseEntity<Resource> downloadReferenceFile(Authentication authentication, @PathVariable String id, @PathVariable String fileId) {
        var meta = studentAssignmentService.getReferenceFileMeta(authentication.getName(), id, fileId);
        Resource resource = studentAssignmentService.downloadReferenceFile(authentication.getName(), id, fileId);
        MediaType mediaType = meta.getMimeType() != null ? MediaType.parseMediaType(meta.getMimeType()) : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + meta.getOriginalFileName() + "\"")
                .body(resource);
    }
}