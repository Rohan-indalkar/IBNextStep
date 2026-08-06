package com.infobeans.ibnextstep.placement;

import com.infobeans.ibnextstep.common.ApiResponse;
import com.infobeans.ibnextstep.common.PagedResponse;
import com.infobeans.ibnextstep.common.exception.ResourceNotFoundException;
import com.infobeans.ibnextstep.common.util.FileStorageService;
import com.infobeans.ibnextstep.placement.dto.PlacementApplicationResponse;
import com.infobeans.ibnextstep.placement.dto.StudentPlacementResponse;
import com.infobeans.ibnextstep.user.Role;
import com.infobeans.ibnextstep.user.User;
import com.infobeans.ibnextstep.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student/placements")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentPlacementController {

    private final PlacementService placementService;
    private final PlacementApplicationService applicationService;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<PagedResponse<StudentPlacementResponse>> browse(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User student = currentStudent(authentication);
        return ApiResponse.success(placementService.browseForStudent(student.getId(), PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<StudentPlacementResponse> getById(Authentication authentication, @PathVariable String id) {
        User student = currentStudent(authentication);
        return ApiResponse.success(placementService.getForStudent(student.getId(), id));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<Resource> downloadPdf(Authentication authentication, @PathVariable String id) {
        User student = currentStudent(authentication);
        Placement placement = placementService.getForPdfDownload(student.getId(), id);
        Resource file = fileStorageService.loadAsResource(placement.getPdfPath());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + placement.getPdfFileName() + "\"")
                .body(file);
    }

    @PostMapping("/{id}/apply")
    public ApiResponse<PlacementApplicationResponse> apply(Authentication authentication, @PathVariable String id) {
        User student = currentStudent(authentication);
        return ApiResponse.success("Application submitted", applicationService.apply(student, id));
    }

    @GetMapping("/applications")
    public ApiResponse<List<PlacementApplicationResponse>> myApplications(Authentication authentication) {
        User student = currentStudent(authentication);
        return ApiResponse.success(applicationService.getMyApplications(student.getId()));
    }

    private User currentStudent(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.STUDENT) {
            throw new ResourceNotFoundException("User not found");
        }
        return user;
    }
}
