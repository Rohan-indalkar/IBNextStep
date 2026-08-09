package com.infobeans.ibnextstep.codingassessment;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AssessmentSessionRepository extends MongoRepository<AssessmentSession, String> {
    List<AssessmentSession> findByAssessmentIdAndStudentId(String assessmentId, String studentId);
    List<AssessmentSession> findByAssessmentId(String assessmentId);
    List<AssessmentSession> findByStatus(SessionStatus status);
    Optional<AssessmentSession> findTopByAssessmentIdAndStudentIdOrderByAttemptNumberDesc(String assessmentId, String studentId);
}
