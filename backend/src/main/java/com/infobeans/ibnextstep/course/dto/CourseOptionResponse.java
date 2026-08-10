package com.infobeans.ibnextstep.course.dto;

import com.infobeans.ibnextstep.course.Course;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Minimal course info for trainer-facing dropdowns (e.g. "Course" select
 * when creating an assignment). Deliberately excludes skillIds/description —
 * CourseController itself stays ADMIN-only for full CRUD.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseOptionResponse {
    private String id;
    private String name;

    public static CourseOptionResponse from(Course course) {
        return CourseOptionResponse.builder()
                .id(course.getId())
                .name(course.getName())
                .build();
    }
}
