package com.infobeans.ibnextstep.material;

import com.infobeans.ibnextstep.batch.Batch;
import com.infobeans.ibnextstep.batch.BatchRepository;
import com.infobeans.ibnextstep.common.PagedResponse;
import com.infobeans.ibnextstep.common.exception.BadRequestException;
import com.infobeans.ibnextstep.common.exception.ResourceNotFoundException;
import com.infobeans.ibnextstep.common.util.FileStorageService;
import com.infobeans.ibnextstep.course.Course;
import com.infobeans.ibnextstep.course.CourseRepository;
import com.infobeans.ibnextstep.material.dto.StudyMaterialResponse;
import com.infobeans.ibnextstep.user.Role;
import com.infobeans.ibnextstep.user.User;
import com.infobeans.ibnextstep.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Student's "Study Materials" view — browse and download whatever's been
 * published to a batch they belong to. Read-only; upload/edit/delete stays
 * on the trainer side (StudyMaterialService).
 */
@Service
@RequiredArgsConstructor
public class StudentStudyMaterialService {

    private final StudyMaterialRepository studyMaterialRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final BatchRepository batchRepository;
    private final FileStorageService fileStorageService;

    public List<StudyMaterialResponse> listAvailable(String studentEmail) {
        User student = getStudent(studentEmail);
        List<String> batchIds = batchRepository.findByStudentIdsContaining(student.getId())
                .stream().map(Batch::getId).toList();
        if (batchIds.isEmpty()) return List.of();

        List<StudyMaterial> materials = studyMaterialRepository.search(
                        StudyMaterialSearchCriteria.builder().status(MaterialStatus.PUBLISHED).build(),
                        Pageable.unpaged())
                .getContent().stream()
                .filter(m -> m.getBatchIds() != null && m.getBatchIds().stream().anyMatch(batchIds::contains))
                .filter(this::isCurrentlyVisible)
                .toList();

        return materials.stream().map(this::enrich).toList();
    }

    public StudyMaterialResponse getOne(String studentEmail, String id) {
        User student = getStudent(studentEmail);
        StudyMaterial material = getVisibleMaterial(id, student);
        return enrich(material);
    }

    public Resource download(String studentEmail, String id, String fileId) {
        User student = getStudent(studentEmail);
        StudyMaterial material = getVisibleMaterial(id, student);
        StudyMaterial.MaterialFile file = material.getFiles().stream()
                .filter(f -> f.getFileId().equals(fileId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));
        return fileStorageService.loadAsResource(file.getStoredPath());
    }

    public StudyMaterial.MaterialFile getFileMeta(String studentEmail, String id, String fileId) {
        User student = getStudent(studentEmail);
        StudyMaterial material = getVisibleMaterial(id, student);
        return material.getFiles().stream()
                .filter(f -> f.getFileId().equals(fileId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));
    }

    // ==================== helpers ====================

    private StudyMaterial getVisibleMaterial(String id, User student) {
        StudyMaterial material = studyMaterialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Study material not found: " + id));
        if (material.getStatus() != MaterialStatus.PUBLISHED) {
            throw new BadRequestException("This study material is not currently available");
        }
        List<Batch> studentBatches = batchRepository.findByStudentIdsContaining(student.getId());
        boolean inAssignedBatch = studentBatches.stream()
                .anyMatch(b -> material.getBatchIds() != null && material.getBatchIds().contains(b.getId()));
        if (!inAssignedBatch) {
            throw new BadRequestException("This study material is not assigned to your batch");
        }
        if (!isCurrentlyVisible(material)) {
            throw new BadRequestException("This study material is not currently visible (check back later or it has expired)");
        }
        return material;
    }

    /** Mirrors StudyMaterialService's effective-status logic: PUBLISHED but still gated by visibleFrom/expiryDate. */
    private boolean isCurrentlyVisible(StudyMaterial material) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        if (material.getExpiryDate() != null && today.isAfter(material.getExpiryDate())) return false;
        if (material.getVisibleFrom() != null && today.isBefore(material.getVisibleFrom())) return false;
        return true;
    }

    private StudyMaterialResponse enrich(StudyMaterial material) {
        var builder = StudyMaterialResponse.fromEntity(material);
        courseRepository.findById(material.getCourseId()).map(Course::getName).ifPresent(builder::courseName);
        if (material.getBatchIds() != null && !material.getBatchIds().isEmpty()) {
            Map<String, String> names = new HashMap<>();
            batchRepository.findAllById(material.getBatchIds()).forEach(b -> names.put(b.getId(), b.getName()));
            builder.batchNames(material.getBatchIds().stream().map(id -> names.getOrDefault(id, id)).toList());
        } else {
            builder.batchNames(List.of());
        }
        builder.effectiveStatus("PUBLISHED");
        return builder.build();
    }

    private User getStudent(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.STUDENT) {
            throw new BadRequestException("Only students can view study materials here");
        }
        return user;
    }
}
