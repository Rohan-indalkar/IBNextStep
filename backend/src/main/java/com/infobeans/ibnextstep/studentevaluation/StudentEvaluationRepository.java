package com.infobeans.ibnextstep.studentevaluation;

import com.infobeans.ibnextstep.user.TrainerType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface StudentEvaluationRepository extends MongoRepository<StudentEvaluation, String> {

    List<StudentEvaluation> findByStudentIdOrderByEvaluatedAtDesc(String studentId);

    Optional<StudentEvaluation> findTopByStudentIdOrderByEvaluatedAtDesc(String studentId);

    /** Used for the combined Technical + Soft-Skill view — latest record per rubric type. */
    Optional<StudentEvaluation> findTopByStudentIdAndTrainerTypeOrderByEvaluatedAtDesc(String studentId, TrainerType trainerType);

    List<StudentEvaluation> findByBatchIdOrderByEvaluatedAtDesc(String batchId);

    /**
     * Used for the batch roster's "already evaluated by me" snapshot —
     * scoped to the viewing trainer's own rubric type, one lookup per student.
     */
    Optional<StudentEvaluation> findTopByStudentIdAndBatchIdAndTrainerTypeOrderByEvaluatedAtDesc(
            String studentId, String batchId, TrainerType trainerType);
}
