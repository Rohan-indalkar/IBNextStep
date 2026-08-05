package com.infobeans.ibnextstep.attendance;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends MongoRepository<AttendanceRecord, String> {

    Optional<AttendanceRecord> findByBatchIdAndStudentIdAndDate(String batchId, String studentId, LocalDate date);

    List<AttendanceRecord> findByBatchIdAndDate(String batchId, LocalDate date);

    List<AttendanceRecord> findByBatchIdAndDateBetween(String batchId, LocalDate start, LocalDate end);

    List<AttendanceRecord> findByStudentIdAndDateBetween(String studentId, LocalDate start, LocalDate end);

    List<AttendanceRecord> findByBatchIdAndStudentIdAndDateBetween(String batchId, String studentId, LocalDate start, LocalDate end);

    boolean existsByBatchIdAndDate(String batchId, LocalDate date);

    // NEW — needed by StudentEvaluationService to compute a student's
    // all-time attendance percentage for the eligibility snapshot.
    List<AttendanceRecord> findByStudentId(String studentId);
}
