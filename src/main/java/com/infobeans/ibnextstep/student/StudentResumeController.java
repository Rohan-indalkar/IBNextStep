package com.infobeans.ibnextstep.student;

import com.infobeans.ibnextstep.common.ApiResponse;
import com.infobeans.ibnextstep.resume.Resume;
import com.infobeans.ibnextstep.resume.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Minimal Student-side surface, just enough for the Resume Review loop to
 * actually work end-to-end ("Student Views Comments" -> "Upload New
 * Version"). The rest of the Student module (dashboard, assignments, etc.)
 * doesn't exist yet — this is scoped only to what Resume Review needs.
 */
@RestController
@RequestMapping("/api/student/resume")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentResumeController {

    private final ResumeService resumeService;

    /** "Student Views Comments" — full version history including every past review's suggestions/score/status. */
    @GetMapping
    public ApiResponse<Resume> myResume(Authentication authentication) {
        return ApiResponse.success(resumeService.myResume(authentication.getName()));
    }

    /**
     * First submission, or "Upload New Version" after NEEDS_CHANGES feedback.
     * consumes = MULTIPART_FORM_DATA + @RequestPart (not @RequestParam) is
     * what makes Swagger UI actually render a file picker for this field —
     * matches the same pattern already used by the Study Material upload.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Resume> upload(@RequestPart("file") MultipartFile file, Authentication authentication) {
        return ApiResponse.success("Resume uploaded", resumeService.uploadVersion(authentication.getName(), file));
    }
}
