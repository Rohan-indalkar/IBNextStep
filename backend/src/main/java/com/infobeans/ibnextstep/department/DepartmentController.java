package com.infobeans.ibnextstep.department;

import com.infobeans.ibnextstep.common.ApiResponse;
import com.infobeans.ibnextstep.common.PagedResponse;
import com.infobeans.ibnextstep.department.dto.DepartmentRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/departments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    public ApiResponse<Department> create(@Valid @RequestBody DepartmentRequest request) {
        return ApiResponse.success("Department created", departmentService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Department> update(@PathVariable String id, @Valid @RequestBody DepartmentRequest request) {
        return ApiResponse.success("Department updated", departmentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        departmentService.delete(id);
        return ApiResponse.success("Department deleted", null);
    }

    @GetMapping
    public ApiResponse<PagedResponse<Department>> search(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(departmentService.search(name, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<Department> getById(@PathVariable String id) {
        return ApiResponse.success(departmentService.getOrThrow(id));
    }
}
