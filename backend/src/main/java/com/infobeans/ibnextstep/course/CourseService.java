package com.infobeans.ibnextstep.course;

import com.infobeans.ibnextstep.audit.AuditLogService;
import com.infobeans.ibnextstep.common.PagedResponse;
import com.infobeans.ibnextstep.common.exception.BadRequestException;
import com.infobeans.ibnextstep.common.exception.ResourceNotFoundException;
import com.infobeans.ibnextstep.course.dto.AssignSkillsRequest;
import com.infobeans.ibnextstep.course.dto.CourseRequest;
import com.infobeans.ibnextstep.course.dto.SkillRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final SkillRepository skillRepository;
    private final AuditLogService auditLogService;

    // --- Skills (master catalog) ---

    public Skill createSkill(SkillRequest request) {
        if (skillRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BadRequestException("A skill with this name already exists");
        }
        Skill skill = Skill.builder().name(request.getName()).description(request.getDescription()).build();
        return skillRepository.save(skill);
    }

    public Skill updateSkill(String id, SkillRequest request) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found: " + id));
        skill.setName(request.getName());
        skill.setDescription(request.getDescription());
        return skillRepository.save(skill);
    }

    public List<Skill> listSkills() {
        return skillRepository.findAll();
    }

    // --- Courses ---

    public Course create(CourseRequest request) {
        Course course = Course.builder()
                .name(request.getName())
                .description(request.getDescription())
                .active(true)
                .createdAt(Instant.now())
                .build();
        course = courseRepository.save(course);
        audit("COURSE_CREATED", "Created course: " + course.getName());
        return course;
    }

    public Course update(String id, CourseRequest request) {
        Course course = getOrThrow(id);
        course.setName(request.getName());
        course.setDescription(request.getDescription());
        course = courseRepository.save(course);
        audit("COURSE_UPDATED", "Updated course: " + course.getName());
        return course;
    }

    public Course assignSkills(String id, AssignSkillsRequest request) {
        Course course = getOrThrow(id);
        List<Skill> validSkills = skillRepository.findByIdIn(request.getSkillIds());
        if (validSkills.size() != request.getSkillIds().size()) {
            throw new BadRequestException("One or more skill IDs are invalid");
        }
        course.setSkillIds(request.getSkillIds());
        course = courseRepository.save(course);
        audit("COURSE_SKILLS_ASSIGNED", "Assigned skills to course: " + course.getName());
        return course;
    }

    public Course deactivate(String id) {
        Course course = getOrThrow(id);
        course.setActive(false);
        course = courseRepository.save(course);
        audit("COURSE_DEACTIVATED", "Deactivated course: " + course.getName());
        return course;
    }

    public PagedResponse<Course> search(String name, Pageable pageable) {
        String q = name == null ? "" : name;
        return PagedResponse.from(courseRepository.findByNameContainingIgnoreCase(q, pageable));
    }

    public Course getOrThrow(String id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + id));
    }

    private void audit(String action, String details) {
        var authEmail = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName() : "system";
        auditLogService.log(null, authEmail, "ADMIN", action, details, null);
    }
}
