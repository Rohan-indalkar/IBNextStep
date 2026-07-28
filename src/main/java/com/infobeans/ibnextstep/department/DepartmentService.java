package com.infobeans.ibnextstep.department;

import com.infobeans.ibnextstep.audit.AuditLogService;
import com.infobeans.ibnextstep.common.PagedResponse;
import com.infobeans.ibnextstep.common.exception.BadRequestException;
import com.infobeans.ibnextstep.common.exception.ResourceNotFoundException;
import com.infobeans.ibnextstep.department.dto.DepartmentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final AuditLogService auditLogService;

    public Department create(DepartmentRequest request) {
        if (departmentRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BadRequestException("A department with this name already exists");
        }
        Department dept = Department.builder()
                .name(request.getName())
                .description(request.getDescription())
                .active(true)
                .createdAt(Instant.now())
                .build();
        dept = departmentRepository.save(dept);
        audit("DEPARTMENT_CREATED", "Created department: " + dept.getName());
        return dept;
    }

    public Department update(String id, DepartmentRequest request) {
        Department dept = getOrThrow(id);
        dept.setName(request.getName());
        dept.setDescription(request.getDescription());
        dept = departmentRepository.save(dept);
        audit("DEPARTMENT_UPDATED", "Updated department: " + dept.getName());
        return dept;
    }

    public void delete(String id) {
        Department dept = getOrThrow(id);
        departmentRepository.deleteById(id);
        audit("DEPARTMENT_DELETED", "Deleted department: " + dept.getName());
    }

    public PagedResponse<Department> search(String name, Pageable pageable) {
        String q = name == null ? "" : name;
        return PagedResponse.from(departmentRepository.findByNameContainingIgnoreCase(q, pageable));
    }

    public Department getOrThrow(String id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + id));
    }

    private void audit(String action, String details) {
        var authEmail = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName() : "system";
        auditLogService.log(null, authEmail, "ADMIN", action, details, null);
    }
}
