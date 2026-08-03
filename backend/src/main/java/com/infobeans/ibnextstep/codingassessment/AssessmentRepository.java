package com.infobeans.ibnextstep.codingassessment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AssessmentRepository extends MongoRepository<Assessment, String> {
    Page<Assessment> findByTrainerId(String trainerId, Pageable pageable);
    Page<Assessment> findByTrainerIdAndTitleContainingIgnoreCase(String trainerId, String title, Pageable pageable);
    List<Assessment> findByBatchIdAndStatus(String batchId, AssessmentStatus status);
    List<Assessment> findByStatusAndEndTimeBefore(AssessmentStatus status, java.time.Instant time);
}
