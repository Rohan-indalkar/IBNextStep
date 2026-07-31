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


@RestController
@RequestMapping("/api/student/resume")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentResumeController {

    private final ResumeService resumeService;

    @GetMapping
    public ApiResponse<Resume> myResume(Authentication authentication) {
        return ApiResponse.success(resumeService.myResume(authentication.getName()));
    }

   
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Resume> upload(@RequestPart("file") MultipartFile file, Authentication authentication) {
        return ApiResponse.success("Resume uploaded", resumeService.uploadVersion(authentication.getName(), file));
    }
}
