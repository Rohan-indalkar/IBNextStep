package com.infobeans.ibnextstep.placement;

import com.infobeans.ibnextstep.common.ApiResponse;
import com.infobeans.ibnextstep.common.PagedResponse;
import com.infobeans.ibnextstep.common.exception.ResourceNotFoundException;
import com.infobeans.ibnextstep.placement.dto.CreatePlacementRequest;
import com.infobeans.ibnextstep.placement.dto.PlacementResponse;
import com.infobeans.ibnextstep.placement.dto.UpdatePlacementRequest;
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

@RestController
@RequestMapping("/api/hr/placements")
@RequiredArgsConstructor
@PreAuthorize("hasRole('HR')")
public class HrPlacementController {

    private final PlacementService placementService;
    private final UserRepository userRepository;

    @PostMapping
    public ApiResponse<PlacementResponse> create(Authentication authentication, @Valid @RequestBody CreatePlacementRequest request) {
        return ApiResponse.success("Placement drive created", placementService.create(currentHr(authentication), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<PlacementResponse> update(Authentication authentication, @PathVariable String id,
                                                  @Valid @RequestBody UpdatePlacementRequest request) {
        return ApiResponse.success("Placement drive updated", placementService.update(currentHr(authentication), id, request));
    }

    @PostMapping(value = "/{id}/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PlacementResponse> uploadPdf(Authentication authentication, @PathVariable String id,
                                                      @RequestParam("file") MultipartFile file) {
        return ApiResponse.success("Placement PDF uploaded", placementService.uploadPdf(currentHr(authentication), id, file));
    }

    @PatchMapping("/{id}/publish")
    public ApiResponse<PlacementResponse> publish(Authentication authentication, @PathVariable String id) {
        return ApiResponse.success("Placement drive published", placementService.publish(currentHr(authentication), id));
    }

    @PatchMapping("/{id}/close")
    public ApiResponse<PlacementResponse> close(Authentication authentication, @PathVariable String id) {
        return ApiResponse.success("Placement drive closed", placementService.close(currentHr(authentication), id));
    }

    @GetMapping
    public ApiResponse<PagedResponse<PlacementResponse>> search(
            @RequestParam(required = false) PlacementStatus status,
            @RequestParam(required = false) PlacementType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(placementService.searchForHr(status, type, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<PlacementResponse> getById(@PathVariable String id) {
        return ApiResponse.success(placementService.getForHr(id));
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
