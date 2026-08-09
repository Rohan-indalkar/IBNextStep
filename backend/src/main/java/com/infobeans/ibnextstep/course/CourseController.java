package com.infobeans.ibnextstep.course;

import com.infobeans.ibnextstep.common.ApiResponse;
import com.infobeans.ibnextstep.common.PagedResponse;
import com.infobeans.ibnextstep.course.dto.AssignSkillsRequest;
import com.infobeans.ibnextstep.course.dto.CourseRequest;
import com.infobeans.ibnextstep.course.dto.SkillRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/courses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    public ApiResponse<Course> create(@Valid @RequestBody CourseRequest request) {
        return ApiResponse.success("Course created", courseService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Course> update(@PathVariable String id, @Valid @RequestBody CourseRequest request) {
        return ApiResponse.success("Course updated", courseService.update(id, request));
    }

    @PutMapping("/{id}/skills")
    public ApiResponse<Course> assignSkills(@PathVariable String id, @Valid @RequestBody AssignSkillsRequest request) {
        return ApiResponse.success("Skills assigned to course", courseService.assignSkills(id, request));
    }

    @PatchMapping("/{id}/deactivate")
    public ApiResponse<Course> deactivate(@PathVariable String id) {
        return ApiResponse.success("Course deactivated", courseService.deactivate(id));
    }

    @GetMapping
    public ApiResponse<PagedResponse<Course>> search(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(courseService.search(name, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<Course> getById(@PathVariable String id) {
        return ApiResponse.success(courseService.getOrThrow(id));
    }

    // --- Skill catalog endpoints ---

    @PostMapping("/skills")
    public ApiResponse<Skill> createSkill(@Valid @RequestBody SkillRequest request) {
        return ApiResponse.success("Skill created", courseService.createSkill(request));
    }

    @PutMapping("/skills/{id}")
    public ApiResponse<Skill> updateSkill(@PathVariable String id, @Valid @RequestBody SkillRequest request) {
        return ApiResponse.success("Skill updated", courseService.updateSkill(id, request));
    }

    @GetMapping("/skills")
    public ApiResponse<List<Skill>> listSkills() {
        return ApiResponse.success(courseService.listSkills());
    }
}
