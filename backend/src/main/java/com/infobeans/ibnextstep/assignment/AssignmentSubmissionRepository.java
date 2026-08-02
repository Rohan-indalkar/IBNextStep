package com.infobeans.ibnextstep.assignment;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AssignmentSubmissionRepository extends MongoRepository<AssignmentSubmission, String> {
    List<AssignmentSubmission> findByAssignmentId(String assignmentId);
    List<AssignmentSubmission> findByStudentId(String studentId);
    Optional<AssignmentSubmission> findByAssignmentIdAndStudentId(String assignmentId, String studentId);
}
