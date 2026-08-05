package com.infobeans.ibnextstep.codingassessment;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends MongoRepository<Submission, String> {
    List<Submission> findBySessionIdAndQuestionId(String sessionId, String questionId);
    Optional<Submission> findTopBySessionIdAndQuestionIdAndRunOnlyFalseOrderByCreatedAtDesc(String sessionId, String questionId);
    List<Submission> findByAssessmentIdAndStudentIdAndRunOnlyFalse(String assessmentId, String studentId);
    List<Submission> findByAssessmentIdAndRunOnlyFalse(String assessmentId);
    // NEW — needed by StudentEvaluationService to aggregate a student's best
    // coding score per question across all assessments for the eligibility snapshot.
    List<Submission> findByStudentIdAndRunOnlyFalse(String studentId);
}
