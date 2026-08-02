package com.infobeans.ibnextstep.attendance;

import com.infobeans.ibnextstep.attendance.dto.AttendanceRecordResponse;
import com.infobeans.ibnextstep.attendance.dto.DailyAttendanceSummaryResponse;
import com.infobeans.ibnextstep.attendance.dto.MarkAttendanceRequest;
import com.infobeans.ibnextstep.attendance.dto.MonthlyBatchAttendanceResponse;
import com.infobeans.ibnextstep.attendance.dto.StudentAttendanceEntry;
import com.infobeans.ibnextstep.attendance.dto.StudentAttendanceSummary;
import com.infobeans.ibnextstep.attendance.dto.StudentForMarkingResponse;
import com.infobeans.ibnextstep.attendance.dto.StudentMonthlyAttendanceResponse;
import com.infobeans.ibnextstep.audit.AuditLogService;
import com.infobeans.ibnextstep.batch.Batch;
import com.infobeans.ibnextstep.batch.BatchRepository;
import com.infobeans.ibnextstep.common.exception.BadRequestException;
import com.infobeans.ibnextstep.common.exception.ResourceNotFoundException;
import com.infobeans.ibnextstep.common.exception.UnauthorizedException;
import com.infobeans.ibnextstep.user.Role;
import com.infobeans.ibnextstep.user.User;
import com.infobeans.ibnextstep.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    /** How many days back a trainer may edit an already-saved attendance date. */
    @Value("${attendance.edit-window-days:7}")
    private int editWindowDays;

    // ============================= TRAINER ============================= //

    /** "Select Batch" -> "Select Date" -> "View Student List", pre-filled with any existing marks. */
    public List<StudentForMarkingResponse> getStudentListForMarking(String trainerId, String batchId, LocalDate date) {
        Batch batch = getBatchOwnedByTrainer(trainerId, batchId);

        Map<String, AttendanceRecord> existingByStudent = new HashMap<>();
        for (AttendanceRecord record : attendanceRecordRepository.findByBatchIdAndDate(batchId, date)) {
            existingByStudent.put(record.getStudentId(), record);
        }

        List<User> students = userRepository.findAllById(batch.getStudentIds());
        List<StudentForMarkingResponse> result = new ArrayList<>();
        for (User student : students) {
            AttendanceRecord existing = existingByStudent.get(student.getId());
            result.add(StudentForMarkingResponse.builder()
                    .studentId(student.getId())
                    .firstName(student.getFirstName())
                    .lastName(student.getLastName())
                    .email(student.getEmail())
                    .status(existing != null ? existing.getStatus() : null)
                    .remarks(existing != null ? existing.getRemarks() : null)
                    .build());
        }
        result.sort(Comparator.comparing(StudentForMarkingResponse::getFirstName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    /** "Search student" within a batch's roster, for the marking screen. */
    public List<StudentForMarkingResponse> searchStudentInBatch(String trainerId, String batchId, LocalDate date, String query) {
        List<StudentForMarkingResponse> all = getStudentListForMarking(trainerId, batchId, date);
        if (query == null || query.isBlank()) return all;
        String q = query.trim().toLowerCase();
        return all.stream()
                .filter(s -> (s.getFirstName() + " " + s.getLastName()).toLowerCase().contains(q)
                        || s.getEmail().toLowerCase().contains(q))
                .toList();
    }

    /** "Mark Attendance" -> "Update Attendance" -> "Save Attendance". Upserts one record per entry. */
    public void markAttendance(String trainerEmail, MarkAttendanceRequest request) {
        Batch batch = getBatchOwnedByTrainer(trainerEmail, request.getBatchId());
        validateEditWindow(request.getDate());

        // Same email-vs-id issue as getBatchOwnedByTrainer — resolve once
        // here too, since the record's markedByTrainerId and the audit log
        // both need the real Mongo id, not the JWT subject.
        User trainer = userRepository.findByEmail(trainerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer not found: " + trainerEmail));

        List<String> validStudentIds = batch.getStudentIds();
        for (StudentAttendanceEntry entry : request.getEntries()) {
            if (!validStudentIds.contains(entry.getStudentId())) {
                throw new BadRequestException("Student " + entry.getStudentId() + " is not part of this batch");
            }

            AttendanceRecord record = attendanceRecordRepository
                    .findByBatchIdAndStudentIdAndDate(request.getBatchId(), entry.getStudentId(), request.getDate())
                    .orElse(AttendanceRecord.builder()
                            .batchId(request.getBatchId())
                            .studentId(entry.getStudentId())
                            .date(request.getDate())
                            .build());

            record.setStatus(entry.getStatus());
            record.setRemarks(entry.getRemarks());
            record.setMarkedByTrainerId(trainer.getId());
            attendanceRecordRepository.save(record);
        }

        auditLogService.log(trainer.getId(), trainer.getEmail(), "TRAINER", "ATTENDANCE_SAVED",
                "Saved attendance for batch " + batch.getName() + " on " + request.getDate(), null);
    }

    /** "Edit attendance of previous dates (if permitted)" — same upsert path, but permission-gated by window. */
    public void editAttendance(String trainerId, MarkAttendanceRequest request) {
        if (!request.getDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Use the regular save endpoint for today's attendance");
        }
        markAttendance(trainerId, request);
    }

    /** "Filter by batch" + "View daily attendance summary". */
    public DailyAttendanceSummaryResponse getDailySummary(String trainerId, String batchId, LocalDate date) {
        Batch batch = getBatchOwnedByTrainer(trainerId, batchId);
        List<AttendanceRecord> records = attendanceRecordRepository.findByBatchIdAndDate(batchId, date);

        int present = (int) records.stream().filter(r -> r.getStatus() == AttendanceStatus.PRESENT).count();
        int absent = (int) records.stream().filter(r -> r.getStatus() == AttendanceStatus.ABSENT).count();
        int late = (int) records.stream().filter(r -> r.getStatus() == AttendanceStatus.LATE).count();
        int total = batch.getStudentIds().size();

        return DailyAttendanceSummaryResponse.builder()
                .batchId(batchId)
                .date(date)
                .totalStudents(total)
                .presentCount(present)
                .absentCount(absent)
                .lateCount(late)
                .notMarkedCount(Math.max(0, total - records.size()))
                .build();
    }

    /** "View monthly attendance summary" for the whole batch, one row per student. */
    public MonthlyBatchAttendanceResponse getMonthlySummary(String trainerId, String batchId, int year, int month) {
        Batch batch = getBatchOwnedByTrainer(trainerId, batchId);
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        List<AttendanceRecord> monthRecords = attendanceRecordRepository.findByBatchIdAndDateBetween(batchId, start, end);
        Map<String, List<AttendanceRecord>> byStudent = new HashMap<>();
        for (AttendanceRecord record : monthRecords) {
            byStudent.computeIfAbsent(record.getStudentId(), k -> new ArrayList<>()).add(record);
        }

        List<User> students = userRepository.findAllById(batch.getStudentIds());
        List<StudentAttendanceSummary> summaries = new ArrayList<>();
        for (User student : students) {
            summaries.add(buildSummary(student, byStudent.getOrDefault(student.getId(), List.of())));
        }
        summaries.sort(Comparator.comparing(StudentAttendanceSummary::getFirstName, String.CASE_INSENSITIVE_ORDER));

        return MonthlyBatchAttendanceResponse.builder()
                .batchId(batchId)
                .year(year)
                .month(month)
                .students(summaries)
                .build();
    }

    private StudentAttendanceSummary buildSummary(User student, List<AttendanceRecord> records) {
        int present = (int) records.stream().filter(r -> r.getStatus() == AttendanceStatus.PRESENT).count();
        int absent = (int) records.stream().filter(r -> r.getStatus() == AttendanceStatus.ABSENT).count();
        int late = (int) records.stream().filter(r -> r.getStatus() == AttendanceStatus.LATE).count();
        int total = records.size();
        double percentage = total == 0 ? 0.0 : Math.round((present * 10000.0) / total) / 100.0;

        return StudentAttendanceSummary.builder()
                .studentId(student.getId())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .totalDays(total)
                .presentCount(present)
                .absentCount(absent)
                .lateCount(late)
                .attendancePercentage(percentage)
                .build();
    }

    private Batch getBatchOwnedByTrainer(String trainerEmail, String batchId) {

        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found"));

        // The Authentication principal name is the trainer's EMAIL (JWT
        // subject), but batch.technicalTrainerId/softSkillTrainerId store
        // the trainer's Mongo _id — resolve email -> id before comparing,
        // or this check fails for every trainer regardless of assignment.
        User trainer = userRepository.findByEmail(trainerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer not found: " + trainerEmail));

        boolean owns = trainer.getId().equals(batch.getTechnicalTrainerId())
                || trainer.getId().equals(batch.getSoftSkillTrainerId());

        if (!owns) {
            throw new UnauthorizedException("You are not assigned to this batch");
        }

        return batch;
    }

    private void validateEditWindow(LocalDate date) {
        if (date.isAfter(LocalDate.now())) {
            throw new BadRequestException("Cannot mark attendance for a future date");
        }
        if (date.isBefore(LocalDate.now().minusDays(editWindowDays))) {
            throw new UnauthorizedException("Editing attendance older than " + editWindowDays + " days is not permitted");
        }
    }

    // ============================= STUDENT ============================= //

    /** "View Attendance Record" — raw list, most recent first, across every batch the student is in. */
    public List<AttendanceRecordResponse> getMyAttendanceRecord(String studentId, LocalDate from, LocalDate to) {
        List<AttendanceRecord> records = attendanceRecordRepository.findByStudentIdAndDateBetween(studentId, from, to);
        return records.stream()
                .sorted(Comparator.comparing(AttendanceRecord::getDate).reversed())
                .map(r -> AttendanceRecordResponse.builder()
                        .date(r.getDate())
                        .status(r.getStatus())
                        .batchId(r.getBatchId())
                        .build())
                .toList();
    }

    /** "View Monthly Attendance" + "View Attendance Percentage", combined. */
    public StudentMonthlyAttendanceResponse getMyMonthlyAttendance(String studentId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        List<AttendanceRecord> records = attendanceRecordRepository.findByStudentIdAndDateBetween(studentId, start, end);
        int present = (int) records.stream().filter(r -> r.getStatus() == AttendanceStatus.PRESENT).count();
        int absent = (int) records.stream().filter(r -> r.getStatus() == AttendanceStatus.ABSENT).count();
        int late = (int) records.stream().filter(r -> r.getStatus() == AttendanceStatus.LATE).count();
        int total = records.size();
        double percentage = total == 0 ? 0.0 : Math.round((present * 10000.0) / total) / 100.0;

        List<AttendanceRecordResponse> recordResponses = records.stream()
                .sorted(Comparator.comparing(AttendanceRecord::getDate))
                .map(r -> AttendanceRecordResponse.builder()
                        .date(r.getDate())
                        .status(r.getStatus())
                        .batchId(r.getBatchId())
                        .build())
                .toList();

        return StudentMonthlyAttendanceResponse.builder()
                .year(year)
                .month(month)
                .totalDays(total)
                .presentCount(present)
                .absentCount(absent)
                .lateCount(late)
                .attendancePercentage(percentage)
                .records(recordResponses)
                .build();
    }

    /**
     * Used by {@link MonthlyAttendanceNotificationScheduler}: every student who has at least
     * one attendance record in the given month, with their computed percentage for that month.
     */
    public List<com.infobeans.ibnextstep.attendance.dto.StudentMonthlyPercentage> getMonthlyPercentagesForAllStudents(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        List<User> students = userRepository.findByRole(com.infobeans.ibnextstep.user.Role.STUDENT,
                org.springframework.data.domain.Pageable.unpaged()).getContent();

        List<com.infobeans.ibnextstep.attendance.dto.StudentMonthlyPercentage> result = new ArrayList<>();
        for (User student : students) {
            List<AttendanceRecord> records = attendanceRecordRepository
                    .findByStudentIdAndDateBetween(student.getId(), start, end);
            if (records.isEmpty()) continue;

            int present = (int) records.stream().filter(r -> r.getStatus() == AttendanceStatus.PRESENT).count();
            int total = records.size();
            double percentage = Math.round((present * 10000.0) / total) / 100.0;

            result.add(com.infobeans.ibnextstep.attendance.dto.StudentMonthlyPercentage.builder()
                    .studentId(student.getId())
                    .totalDays(total)
                    .presentCount(present)
                    .attendancePercentage(percentage)
                    .build());
        }
        return result;
    }
}
