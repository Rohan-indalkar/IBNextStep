package com.infobeans.ibnextstep.material;

import com.infobeans.ibnextstep.common.ApiResponse;
import com.infobeans.ibnextstep.material.dto.StudyMaterialResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Student's "Study Materials" view — browse and download whatever's published to their batch. */
@RestController
@RequestMapping("/api/student/study-materials")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentStudyMaterialController {

    private final StudentStudyMaterialService studentStudyMaterialService;

    @GetMapping
    public ApiResponse<List<StudyMaterialResponse>> listAvailable(Authentication authentication) {
        return ApiResponse.success(studentStudyMaterialService.listAvailable(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ApiResponse<StudyMaterialResponse> getOne(Authentication authentication, @PathVariable String id) {
        return ApiResponse.success(studentStudyMaterialService.getOne(authentication.getName(), id));
    }

    @GetMapping("/{id}/files/{fileId}/download")
    public ResponseEntity<Resource> download(
            Authentication authentication, @PathVariable String id, @PathVariable String fileId) {
        var meta = studentStudyMaterialService.getFileMeta(authentication.getName(), id, fileId);
        Resource resource = studentStudyMaterialService.download(authentication.getName(), id, fileId);

        MediaType mediaType = meta.getMimeType() != null
                ? MediaType.parseMediaType(meta.getMimeType())
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + meta.getOriginalFileName() + "\"")
                .body(resource);
    }
}
