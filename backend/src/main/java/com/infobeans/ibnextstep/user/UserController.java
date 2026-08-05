package com.infobeans.ibnextstep.user;

import com.infobeans.ibnextstep.common.ApiResponse;
import com.infobeans.ibnextstep.common.BulkImportResult;
import com.infobeans.ibnextstep.common.PagedResponse;
import com.infobeans.ibnextstep.user.dto.CreateUserRequest;
import com.infobeans.ibnextstep.user.dto.UpdateUserRequest;
import com.infobeans.ibnextstep.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success("User created and credentials emailed", userService.createUser(request));
    }

    @PostMapping(value = "/bulk-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<BulkImportResult> bulkImport(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success("Bulk import processed", userService.bulkImportUsers(file));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(@PathVariable String id, @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.success("User updated", userService.updateUser(id, request));
    }

    @PatchMapping("/{id}/activate")
    public ApiResponse<UserResponse> activate(@PathVariable String id) {
        return ApiResponse.success("User activated", userService.setStatus(id, true));
    }

    @PatchMapping("/{id}/deactivate")
    public ApiResponse<UserResponse> deactivate(@PathVariable String id) {
        return ApiResponse.success("User deactivated", userService.setStatus(id, false));
    }

    @PostMapping("/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable String id) {
        userService.resetPassword(id);
        return ApiResponse.success("Temporary password emailed to user", null);
    }

    @GetMapping
    public ApiResponse<PagedResponse<UserResponse>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(userService.search(query, role, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getById(@PathVariable String id) {
        return ApiResponse.success(userService.getById(id));
    }
}
