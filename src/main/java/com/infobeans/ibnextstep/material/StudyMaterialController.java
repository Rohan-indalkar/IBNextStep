package com.infobeans.ibnextstep.material;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infobeans.ibnextstep.common.ApiResponse;
import com.infobeans.ibnextstep.common.PagedResponse;
import com.infobeans.ibnextstep.material.dto.SchedulePublishRequest;
import com.infobeans.ibnextstep.material.dto.StudyMaterialRequest;
import com.infobeans.ibnextstep.material.dto.StudyMaterialResponse;
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
 * Trainer's "Study Materials" workspace — upload, edit, publish/schedule,
 * search & download. All endpoints operate on the calling trainer's own
 * uploads; ownership is enforced in StudyMaterialService.
 */
@RestController
@RequestMapping("/api/trainer/study-materials")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TRAINER')")
public class StudyMaterialController {

    private final StudyMaterialService studyMaterialService;

    /**
     * Upload Study Material.
     * multipart/form-data with two parts:
     *  - "data"  : JSON body matching StudyMaterialRequest (Content-Type: application/json on the part)
     *  - "files" : zero or more files (only used when contentType is file-based; supports multiple files)
     */

    private final ObjectMapper objectMapper;
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<StudyMaterialResponse> upload(
            Authentication authentication,
            @RequestPart("data") String data,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) throws Exception {

        StudyMaterialRequest request =
                objectMapper.readValue(data, StudyMaterialRequest.class);

        return ApiResponse.success(
                "Study material saved",
                studyMaterialService.upload(authentication.getName(), request, files)
        );
    }

    @PostMapping(value = "/test", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String test(
            @RequestPart("data") String data,
            @RequestPart(value = "files", required = false) MultipartFile file) {

        System.out.println("DATA = " + data);
        return "OK";
    }
    /** Update / Delete Materials — update metadata and optionally append new files. */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<StudyMaterialResponse> update(
            Authentication authentication,
            @PathVariable String id,
            @Valid @RequestPart("data") StudyMaterialRequest data,
            @RequestPart(value = "files", required = false) List<MultipartFile> newFiles) {
        return ApiResponse.success("Study material updated", studyMaterialService.update(authentication.getName(), id, data, newFiles));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Authentication authentication, @PathVariable String id) {
        studyMaterialService.delete(authentication.getName(), id);
        return ApiResponse.success("Study material deleted", null);
    }

    @DeleteMapping("/{id}/files/{fileId}")
    public ApiResponse<StudyMaterialResponse> deleteFile(
            Authentication authentication, @PathVariable String id, @PathVariable String fileId) {
        return ApiResponse.success("File removed", studyMaterialService.deleteFile(authentication.getName(), id, fileId));
    }

    // ---------- Publish options ----------

    @PatchMapping("/{id}/publish")
    public ApiResponse<StudyMaterialResponse> publishNow(Authentication authentication, @PathVariable String id) {
        return ApiResponse.success("Study material published", studyMaterialService.publishNow(authentication.getName(), id));
    }

    @PatchMapping("/{id}/schedule")
    public ApiResponse<StudyMaterialResponse> schedule(
            Authentication authentication, @PathVariable String id, @Valid @RequestBody SchedulePublishRequest request) {
        return ApiResponse.success("Publish scheduled", studyMaterialService.schedule(authentication.getName(), id, request));
    }

    @PatchMapping("/{id}/unpublish")
    public ApiResponse<StudyMaterialResponse> unpublish(Authentication authentication, @PathVariable String id) {
        return ApiResponse.success("Study material moved back to draft", studyMaterialService.unpublish(authentication.getName(), id));
    }

    // ---------- Read / Search / Filter ----------

    @GetMapping("/{id}")
    public ApiResponse<StudyMaterialResponse> getOne(Authentication authentication, @PathVariable String id) {
        return ApiResponse.success(studyMaterialService.getOne(authentication.getName(), id));
    }

    @GetMapping
    public ApiResponse<PagedResponse<StudyMaterialResponse>> search(
            Authentication authentication,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) TrainerType skillType,
            @RequestParam(required = false) DifficultyLevel difficultyLevel,
            @RequestParam(required = false) ContentType contentType,
            @RequestParam(required = false) MaterialStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        StudyMaterialSearchCriteria criteria = StudyMaterialSearchCriteria.builder()
                .search(search).courseId(courseId).batchId(batchId).skillType(skillType)
                .difficultyLevel(difficultyLevel).contentType(contentType).status(status)
                .build();

        return ApiResponse.success(studyMaterialService.search(authentication.getName(), criteria, PageRequest.of(page, size)));
    }

    // ---------- Download ----------

    @GetMapping("/{id}/files/{fileId}/download")
    public ResponseEntity<Resource> download(
            Authentication authentication, @PathVariable String id, @PathVariable String fileId) {
        var meta = studyMaterialService.getFileMeta(authentication.getName(), id, fileId);
        Resource resource = studyMaterialService.download(authentication.getName(), id, fileId);

        MediaType mediaType = meta.getMimeType() != null
                ? MediaType.parseMediaType(meta.getMimeType())
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + meta.getOriginalFileName() + "\"")
                .body(resource);
    }
}
