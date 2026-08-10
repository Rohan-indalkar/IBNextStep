package com.infobeans.ibnextstep.course;

import com.infobeans.ibnextstep.common.ApiResponse;
import com.infobeans.ibnextstep.course.dto.CourseOptionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only course list for trainer-facing dropdowns (e.g. the "Course"
 * select when creating an assignment). CourseController itself
 * (/api/admin/courses) stays ADMIN-only for create/update/deactivate —
 * this is a deliberately narrow, separate surface for trainers.
 */
@RestController
@RequestMapping("/api/trainer/courses")   // TrainerCourseController, @PreAuthorize("hasRole('TRAINER')")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TRAINER')")
public class TrainerCourseController {

    private final CourseRepository courseRepository;

    @GetMapping
    public ApiResponse<List<CourseOptionResponse>> list() {
        return ApiResponse.success(
                courseRepository.findAll().stream()
                        .filter(Course::isActive)
                        .map(CourseOptionResponse::from)
                        .toList()
        );
    }
}
