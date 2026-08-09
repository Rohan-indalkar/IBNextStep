package com.infobeans.ibnextstep.batch;

import com.infobeans.ibnextstep.audit.AuditLogService;
import com.infobeans.ibnextstep.batch.dto.AssignStudentsRequest;
import com.infobeans.ibnextstep.batch.dto.AssignTrainerRequest;
import com.infobeans.ibnextstep.batch.dto.BatchRequest;
import com.infobeans.ibnextstep.batch.dto.TimetableEntryRequest;
import com.infobeans.ibnextstep.common.BulkImportResult;
import com.infobeans.ibnextstep.common.PagedResponse;
import com.infobeans.ibnextstep.common.exception.BadRequestException;
import com.infobeans.ibnextstep.common.exception.ResourceNotFoundException;
import com.infobeans.ibnextstep.user.Role;
import com.infobeans.ibnextstep.user.TrainerType;
import com.infobeans.ibnextstep.user.User;
import com.infobeans.ibnextstep.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BatchService {

    private final BatchRepository batchRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public Batch create(BatchRequest request) {
        Batch batch = Batch.builder()
                .name(request.getName())
                .courseId(request.getCourseId())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(Batch.BatchStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();
        batch = batchRepository.save(batch);
        audit("BATCH_CREATED", "Created batch: " + batch.getName());
        return batch;
    }

    public Batch update(String id, BatchRequest request) {
        Batch batch = getOrThrow(id);
        batch.setName(request.getName());
        batch.setCourseId(request.getCourseId());
        batch.setStartDate(request.getStartDate());
        batch.setEndDate(request.getEndDate());
        batch = batchRepository.save(batch);
        audit("BATCH_UPDATED", "Updated batch: " + batch.getName());
        return batch;
    }

    public Batch assignTechnicalTrainer(String batchId, AssignTrainerRequest request) {
        User trainer = validateTrainer(request.getTrainerId(), TrainerType.TECHNICAL);
        Batch batch = getOrThrow(batchId);
        batch.setTechnicalTrainerId(trainer.getId());
        batch = batchRepository.save(batch);
        audit("BATCH_TECHNICAL_TRAINER_ASSIGNED", "Assigned " + trainer.getEmail() + " to batch " + batch.getName());
        return batch;
    }

    public Batch assignSoftSkillTrainer(String batchId, AssignTrainerRequest request) {
        User trainer = validateTrainer(request.getTrainerId(), TrainerType.SOFT_SKILL);
        Batch batch = getOrThrow(batchId);
        batch.setSoftSkillTrainerId(trainer.getId());
        batch = batchRepository.save(batch);
        audit("BATCH_SOFT_SKILL_TRAINER_ASSIGNED", "Assigned " + trainer.getEmail() + " to batch " + batch.getName());
        return batch;
    }

    public Batch changeTrainer(String batchId, TrainerType type, AssignTrainerRequest request) {
        return type == TrainerType.TECHNICAL
                ? assignTechnicalTrainer(batchId, request)
                : assignSoftSkillTrainer(batchId, request);
    }

    public Batch assignStudents(String batchId, AssignStudentsRequest request) {
        Batch batch = getOrThrow(batchId);
        for (String studentId : request.getStudentIds()) {
            User student = userRepository.findById(studentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
            if (student.getRole() != Role.STUDENT) {
                throw new BadRequestException(student.getEmail() + " is not a student account");
            }
        }
        List<String> merged = new ArrayList<>(batch.getStudentIds());
        for (String id : request.getStudentIds()) {
            if (!merged.contains(id)) merged.add(id);
        }
        batch.setStudentIds(merged);
        batch = batchRepository.save(batch);
        audit("BATCH_STUDENTS_ASSIGNED", "Assigned " + request.getStudentIds().size() + " students to batch " + batch.getName());
        return batch;
    }

    public Batch getTimetable(String batchId) {
        return getOrThrow(batchId);
    }

    public Batch addTimetableEntry(String batchId, TimetableEntryRequest request) {
        Batch batch = getOrThrow(batchId);

        User trainer = userRepository.findById(request.getTrainerId())
                .orElseThrow(() -> new ResourceNotFoundException("Trainer not found: " + request.getTrainerId()));
        if (trainer.getRole() != Role.TRAINER) {
            throw new BadRequestException(trainer.getEmail() + " is not a trainer account");
        }

        Batch.TimetableEntry entry = Batch.TimetableEntry.builder()
                .date(request.getDate())
                .topic(request.getTopic())
                .trainerId(request.getTrainerId())
                .build();

        List<Batch.TimetableEntry> updated = new ArrayList<>(batch.getTimetable());
        updated.add(entry);
        batch.setTimetable(updated);
        batch = batchRepository.save(batch);
        audit("BATCH_TIMETABLE_UPDATED", "Added timetable entry (" + request.getDate() + ": " + request.getTopic() + ") to batch " + batch.getName());
        return batch;
    }

    public Batch removeStudent(String batchId, String studentId) {
        Batch batch = getOrThrow(batchId);
        List<String> updated = new ArrayList<>(batch.getStudentIds());
        updated.remove(studentId);
        batch.setStudentIds(updated);
        batch = batchRepository.save(batch);
        audit("BATCH_STUDENT_REMOVED", "Removed student " + studentId + " from batch " + batch.getName());
        return batch;
    }

    public Batch deactivate(String id) {
        Batch batch = getOrThrow(id);
        batch.setStatus(Batch.BatchStatus.INACTIVE);
        batch = batchRepository.save(batch);
        audit("BATCH_DEACTIVATED", "Deactivated batch: " + batch.getName());
        return batch;
    }

    public PagedResponse<Batch> search(String name, Pageable pageable) {
        String q = name == null ? "" : name;
        return PagedResponse.from(batchRepository.findByNameContainingIgnoreCase(q, pageable));
    }

    public Batch getOrThrow(String id) {
        return batchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + id));
    }

    /** CSV columns expected: email */
    public BulkImportResult bulkImportStudents(String batchId, MultipartFile file) {
        Batch batch = getOrThrow(batchId);
        BulkImportResult result = new BulkImportResult();
        List<String> studentIds = new ArrayList<>(batch.getStudentIds());

        try (var reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            CSVParser parser = CSVFormat.DEFAULT.builder()
                    .setHeader().setSkipHeaderRecord(true).setTrim(true)
                    .build()
                    .parse(reader);

            int rowNum = 1;
            for (CSVRecord record : parser) {
                rowNum++;
                result.setTotalRows(result.getTotalRows() + 1);
                try {
                    String email = record.get("email");
                    Optional<User> studentOpt = userRepository.findByEmail(email);
                    if (studentOpt.isEmpty()) {
                        result.recordFailure(rowNum, "No account found for email: " + email);
                        continue;
                    }
                    User student = studentOpt.get();
                    if (student.getRole() != Role.STUDENT) {
                        result.recordFailure(rowNum, email + " is not a student account");
                        continue;
                    }
                    if (!studentIds.contains(student.getId())) {
                        studentIds.add(student.getId());
                    }
                    result.recordSuccess();
                } catch (Exception e) {
                    result.recordFailure(rowNum, e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to read CSV file: " + e.getMessage());
        }

        batch.setStudentIds(studentIds);
        batchRepository.save(batch);
        audit("BATCH_STUDENTS_BULK_IMPORTED", "Bulk imported students into batch " + batch.getName()
                + ": " + result.getSuccessCount() + " succeeded, " + result.getFailureCount() + " failed");
        return result;
    }

    private User validateTrainer(String trainerId, TrainerType expectedType) {
        User trainer = userRepository.findById(trainerId)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer not found: " + trainerId));
        if (trainer.getRole() != Role.TRAINER) {
            throw new BadRequestException(trainer.getEmail() + " is not a trainer account");
        }
        if (trainer.getTrainerType() != expectedType) {
            throw new BadRequestException(trainer.getEmail() + " is not a " + expectedType + " trainer");
        }
        return trainer;
    }

    private void audit(String action, String details) {
        var authEmail = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName() : "system";
        auditLogService.log(null, authEmail, "ADMIN", action, details, null);
    }
}
