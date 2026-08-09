package com.infobeans.ibnextstep.placement;

import com.infobeans.ibnextstep.common.ApiResponse;
import com.infobeans.ibnextstep.common.PagedResponse;
import com.infobeans.ibnextstep.common.exception.ResourceNotFoundException;
import com.infobeans.ibnextstep.placement.dto.CompanyResponse;
import com.infobeans.ibnextstep.placement.dto.CreateCompanyRequest;
import com.infobeans.ibnextstep.placement.dto.UpdateCompanyRequest;
import com.infobeans.ibnextstep.user.Role;
import com.infobeans.ibnextstep.user.User;
import com.infobeans.ibnextstep.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * HR-only company management. Companies are onboarded here before HR can
 * create a placement drive against them.
 */
@RestController
@RequestMapping("/api/hr/companies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('HR')")
public class CompanyController {

    private final CompanyService companyService;
    private final UserRepository userRepository;

    @PostMapping
    public ApiResponse<CompanyResponse> create(Authentication authentication, @Valid @RequestBody CreateCompanyRequest request) {
        return ApiResponse.success("Company created", companyService.create(currentHr(authentication), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CompanyResponse> update(Authentication authentication, @PathVariable String id,
                                                @Valid @RequestBody UpdateCompanyRequest request) {
        return ApiResponse.success("Company updated", companyService.update(currentHr(authentication), id, request));
    }

    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CompanyResponse> uploadLogo(Authentication authentication, @PathVariable String id,
                                                    @RequestParam("file") MultipartFile file) {
        return ApiResponse.success("Company logo updated", companyService.uploadLogo(currentHr(authentication), id, file));
    }

    @PatchMapping("/{id}/activate")
    public ApiResponse<CompanyResponse> activate(Authentication authentication, @PathVariable String id) {
        return ApiResponse.success("Company activated", companyService.setActive(currentHr(authentication), id, true));
    }

    @PatchMapping("/{id}/deactivate")
    public ApiResponse<CompanyResponse> deactivate(Authentication authentication, @PathVariable String id) {
        return ApiResponse.success("Company deactivated", companyService.setActive(currentHr(authentication), id, false));
    }

    @GetMapping
    public ApiResponse<PagedResponse<CompanyResponse>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(companyService.search(query, active, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<CompanyResponse> getById(@PathVariable String id) {
        return ApiResponse.success(companyService.getById(id));
    }

    private User currentHr(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.HR) {
            throw new ResourceNotFoundException("User not found");
        }
        return user;
    }
}
