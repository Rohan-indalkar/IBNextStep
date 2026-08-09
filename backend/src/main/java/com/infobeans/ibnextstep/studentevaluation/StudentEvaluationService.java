package com.infobeans.ibnextstep.studentevaluation;

import com.infobeans.ibnextstep.audit.AuditLogService;
import com.infobeans.ibnextstep.batch.Batch;
import com.infobeans.ibnextstep.batch.BatchRepository;
import com.infobeans.ibnextstep.common.exception.BadRequestException;
import com.infobeans.ibnextstep.common.exception.ResourceNotFoundException;
import com.infobeans.ibnextstep.notification.NotificationService;
import com.infobeans.ibnextstep.studentevaluation.dto.BatchEvaluationOverviewResponse;
import com.infobeans.ibnextstep.studentevaluation.dto.CombinedEvaluationResponse;
import com.infobeans.ibnextstep.studentevaluation.dto.EvaluationMetricsResponse;
import com.infobeans.ibnextstep.studentevaluation.dto.EvaluationRubricConfigResponse;
import com.infobeans.ibnextstep.studentevaluation.dto.EvaluationRubricResponse;
import com.infobeans.ibnextstep.studentevaluation.dto.StudentEligibilitySnapshot;
import com.infobeans.ibnextstep.studentevaluation.dto.StudentEvaluationResponse;
import com.infobeans.ibnextstep.studentevaluation.dto.SubmitEvaluationRequest;
import com.infobeans.ibnextstep.studentevaluation.dto.UpdateEvaluationRequest;
import com.infobeans.ibnextstep.studentevaluation.dto.UpdateEvaluationRubricRequest;
import com.infobeans.ibnextstep.user.Role;
import com.infobeans.ibnextstep.user.TrainerType;
import com.infobeans.ibnextstep.user.User;
import com.infobeans.ibnextstep.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudentEvaluationService {

    private final StudentEvaluationRepository evaluationRepository;
    private final EvaluationRubricConfigRepository rubricConfigRepository;
    private final UserRepository userRepository;
    private final BatchRepository batchRepository;
    private final StudentMetricsService studentMetricsService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    // ---------- Eligibility thresholds — configurable, sensible defaults ----------
    @Value("${placement.eligibility.min-attendance-percentage:75}")
    private double minAttendancePercentage;

    @Value("${placement.eligibility.min-quiz-percentage:60}")
    private double minQuizPercentage;

    @Value("${placement.eligibility.min-coding-percentage:50}")
    private double minCodingPercentage;

    @Value("${placement.eligibility.min-mock-interview-rating:6}")
    private double minMockInterviewRating;

    // ---------- Default rubrics — used whenever an admin hasn't configured a
    // custom skill list for that trainer type via EvaluationRubricConfig.
    // Existing evaluation records are never affected by later rubric edits;
    // each record permanently keeps the skill names it was scored against. ----------
    private static final List<String> DEFAULT_TECHNICAL_RUBRIC = List.of(
            "Problem Solving", "Code Quality", "Debugging Skills",
            "Technical Concept Clarity", "Tool/Technology Proficiency"
    );

    private static final List<String> DEFAULT_SOFT_SKILL_RUBRIC = List.of(
            "Communication", "Presentation Skills", "Teamwork",
            "Confidence", "Professionalism"
    );

    public EvaluationRubricResponse getRubric(String trainerEmail) {
        User trainer = getTrainer(trainerEmail);
        return EvaluationRubricResponse.builder()
                .trainerType(trainer.getTrainerType())
                .skills(getRubricSkills(trainer.getTrainerType()))
                .build();
    }

    public EvaluationMetricsResponse getMetrics(String trainerEmail, String studentId) {
        User trainer = getTrainer(trainerEmail);
        User student = getStudent(studentId);
        assertTrainerAssignedToStudent(trainer, studentId);
        Metrics metrics = computeMetrics(studentId);

        return EvaluationMetricsResponse.builder()
                .studentId(studentId)
                .studentName(fullName(student))
                .attendancePercentage(metrics.attendancePercentage)
                .avgQuizPercentage(metrics.avgQuizPercentage)
                .avgCodingPercentage(metrics.avgCodingPercentage)
                .avgMockInterviewRating(metrics.avgMockInterviewRating)
                .systemEligible(metrics.systemEligible)
                .systemIneligibilityReasons(metrics.reasons)
                .minAttendancePercentage(minAttendancePercentage)
                .minQuizPercentage(minQuizPercentage)
                .minCodingPercentage(minCodingPercentage)
                .minMockInterviewRating(minMockInterviewRating)
                .build();
    }

    public StudentEvaluationResponse submit(String trainerEmail, String studentId, SubmitEvaluationRequest request) {
        User trainer = getTrainer(trainerEmail);
        User student = getStudent(studentId);
        assertTrainerAssignedToStudent(trainer, studentId);

        validateSkillScores(trainer.getTrainerType(), request.getSkillScores());

        Metrics metrics = computeMetrics(studentId);
        boolean finalEligible = resolveFinalEligible(request.getFinalEligibleOverride(), request.getOverrideReason(), metrics.systemEligible);
        double overallRubricScore = average(request.getSkillScores());

        String batchId = batchRepository.findByStudentIdsContaining(studentId).stream()
                .findFirst()
                .map(Batch::getId)
                .orElse(null);

        StudentEvaluation evaluation = StudentEvaluation.builder()
                .studentId(studentId)
                .batchId(batchId)
                .trainerId(trainer.getId())
                .trainerName(fullName(trainer))
                .trainerType(trainer.getTrainerType())
                .attendancePercentage(metrics.attendancePercentage)
                .avgQuizPercentage(metrics.avgQuizPercentage)
                .avgCodingPercentage(metrics.avgCodingPercentage)
                .avgMockInterviewRating(metrics.avgMockInterviewRating)
                .systemEligible(metrics.systemEligible)
                .systemIneligibilityReasons(metrics.reasons)
                .skillScores(request.getSkillScores())
                .overallRubricScore(round(overallRubricScore))
                .remarks(request.getRemarks())
                .finalEligible(finalEligible)
                .overrideReason(request.getOverrideReason())
                .evaluatedAt(Instant.now())
                .edited(false)
                .build();

        evaluation = evaluationRepository.save(evaluation);

        applyPlacementReadinessScore(student, overallRubricScore);

        auditLogService.log(trainer.getId(), trainer.getEmail(), trainer.getRole().name(),
                "STUDENT_EVALUATION_SUBMITTED",
                "Submitted a " + trainer.getTrainerType() + " evaluation for " + fullName(student) + " (" + studentId + ")",
                null);

        notifyStudent(student, "New evaluation submitted",
                "Your " + trainerTypeLabel(trainer.getTrainerType()) + " trainer " + fullName(trainer)
                        + " has submitted a new evaluation for you. Eligibility: "
                        + (evaluation.isFinalEligible() ? "Eligible" : "Not yet eligible") + ".");

        return toResponse(evaluation);
    }

    /**
     * Corrects a mistake on an evaluation the trainer already submitted.
     * Only the original trainer can edit their own record. The system
     * metrics snapshot is recomputed fresh so the correction reflects the
     * student's current standing rather than the numbers from the original
     * submission.
     */
    public StudentEvaluationResponse update(String trainerEmail, String evaluationId, UpdateEvaluationRequest request) {
        User trainer = getTrainer(trainerEmail);
        StudentEvaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation not found: " + evaluationId));

        if (!evaluation.getTrainerId().equals(trainer.getId())) {
            throw new BadRequestException("You can only edit evaluations you submitted yourself");
        }

        User student = getStudent(evaluation.getStudentId());

        validateSkillScores(evaluation.getTrainerType(), request.getSkillScores());

        Metrics metrics = computeMetrics(evaluation.getStudentId());
        boolean finalEligible = resolveFinalEligible(request.getFinalEligibleOverride(), request.getOverrideReason(), metrics.systemEligible);
        double overallRubricScore = average(request.getSkillScores());

        evaluation.setAttendancePercentage(metrics.attendancePercentage);
        evaluation.setAvgQuizPercentage(metrics.avgQuizPercentage);
        evaluation.setAvgCodingPercentage(metrics.avgCodingPercentage);
        evaluation.setAvgMockInterviewRating(metrics.avgMockInterviewRating);
        evaluation.setSystemEligible(metrics.systemEligible);
        evaluation.setSystemIneligibilityReasons(metrics.reasons);
        evaluation.setSkillScores(request.getSkillScores());
        evaluation.setOverallRubricScore(round(overallRubricScore));
        evaluation.setRemarks(request.getRemarks());
        evaluation.setFinalEligible(finalEligible);
        evaluation.setOverrideReason(request.getOverrideReason());
        evaluation.setEdited(true);
        evaluation.setUpdatedAt(Instant.now());
        evaluation.setLastEditedBy(fullName(trainer));

        evaluation = evaluationRepository.save(evaluation);

        applyPlacementReadinessScore(student, overallRubricScore);

        auditLogService.log(trainer.getId(), trainer.getEmail(), trainer.getRole().name(),
                "STUDENT_EVALUATION_UPDATED",
                "Corrected the " + evaluation.getTrainerType() + " evaluation " + evaluationId
                        + " for " + fullName(student) + " (" + evaluation.getStudentId() + ")",
                null);

        notifyStudent(student, "Evaluation updated",
                fullName(trainer) + " updated your " + trainerTypeLabel(evaluation.getTrainerType())
                        + " evaluation. Eligibility: " + (evaluation.isFinalEligible() ? "Eligible" : "Not yet eligible") + ".");

        return toResponse(evaluation);
    }

    public List<StudentEvaluationResponse> getHistoryForStudent(String studentId) {
        return evaluationRepository.findByStudentIdOrderByEvaluatedAtDesc(studentId).stream()
                .map(this::toResponse)
                .toList();
    }

    public StudentEvaluationResponse getOne(String id) {
        StudentEvaluation evaluation = evaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation not found: " + id));
        return toResponse(evaluation);
    }

    /** Same lookup as {@link #getOne}, but scoped so a trainer can only export/view their own submissions. */
    public StudentEvaluationResponse getOneOwnedByTrainer(String trainerEmail, String id) {
        User trainer = getTrainer(trainerEmail);
        StudentEvaluation evaluation = evaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation not found: " + id));
        if (!evaluation.getTrainerId().equals(trainer.getId())) {
            throw new BadRequestException("You can only access evaluations you submitted yourself");
        }
        return toResponse(evaluation);
    }

    /**
     * The "list my batch's students" roster — every student in the trainer's
     * batch with a live eligibility snapshot and whether this trainer's own
     * rubric type has already evaluated them, so the trainer can pick who to
     * evaluate next without needing to already know a student's ID.
     */
    public BatchEvaluationOverviewResponse getBatchOverview(String trainerEmail, String batchId) {
        User trainer = getTrainer(trainerEmail);
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));

        boolean assignedToBatch = trainer.getId().equals(batch.getTechnicalTrainerId())
                || trainer.getId().equals(batch.getSoftSkillTrainerId());
        if (!assignedToBatch) {
            throw new BadRequestException("You are not assigned to this batch");
        }

        List<User> students = new ArrayList<>();
        userRepository.findAllById(batch.getStudentIds()).forEach(students::add);
        students = students.stream()
                .filter(u -> u.getRole() == Role.STUDENT)
                .sorted(Comparator.comparing(u -> fullName(u).toLowerCase()))
                .toList();

        List<StudentEligibilitySnapshot> snapshots = new ArrayList<>();
        int evaluatedByMe = 0;

        for (User student : students) {
            Metrics metrics = computeMetrics(student.getId());
            Optional<StudentEvaluation> lastByMyRubric = evaluationRepository
                    .findTopByStudentIdAndBatchIdAndTrainerTypeOrderByEvaluatedAtDesc(
                            student.getId(), batchId, trainer.getTrainerType());

            boolean evaluated = lastByMyRubric.isPresent();
            if (evaluated) {
                evaluatedByMe++;
            }

            snapshots.add(StudentEligibilitySnapshot.builder()
                    .studentId(student.getId())
                    .studentName(fullName(student))
                    .email(student.getEmail())
                    .attendancePercentage(metrics.attendancePercentage)
                    .avgQuizPercentage(metrics.avgQuizPercentage)
                    .avgCodingPercentage(metrics.avgCodingPercentage)
                    .avgMockInterviewRating(metrics.avgMockInterviewRating)
                    .systemEligible(metrics.systemEligible)
                    .systemIneligibilityReasons(metrics.reasons)
                    .evaluatedByMyRubric(evaluated)
                    .lastEvaluatedAt(lastByMyRubric.map(StudentEvaluation::getEvaluatedAt).orElse(null))
                    .lastOverallRubricScore(lastByMyRubric.map(StudentEvaluation::getOverallRubricScore).orElse(null))
                    .lastFinalEligible(lastByMyRubric.map(StudentEvaluation::isFinalEligible).orElse(null))
                    .lastEvaluationId(lastByMyRubric.map(StudentEvaluation::getId).orElse(null))
                    .rubricType(trainer.getTrainerType())
                    .build());
        }

        return BatchEvaluationOverviewResponse.builder()
                .batchId(batch.getId())
                .batchName(batch.getName())
                .totalStudents(snapshots.size())
                .evaluatedByMe(evaluatedByMe)
                .pendingEvaluationByMe(snapshots.size() - evaluatedByMe)
                .students(snapshots)
                .build();
    }

    /**
     * The student's latest Technical evaluation and latest Soft-Skill
     * evaluation side by side, with a blended score/verdict across both.
     */
    public CombinedEvaluationResponse getCombinedView(String studentId) {
        User student = getStudent(studentId);

        StudentEvaluationResponse technical = evaluationRepository
                .findTopByStudentIdAndTrainerTypeOrderByEvaluatedAtDesc(studentId, TrainerType.TECHNICAL)
                .map(this::toResponse)
                .orElse(null);
        StudentEvaluationResponse softSkill = evaluationRepository
                .findTopByStudentIdAndTrainerTypeOrderByEvaluatedAtDesc(studentId, TrainerType.SOFT_SKILL)
                .map(this::toResponse)
                .orElse(null);

        List<Double> scores = new ArrayList<>();
        if (technical != null && technical.getOverallRubricScore() != null) scores.add(technical.getOverallRubricScore());
        if (softSkill != null && softSkill.getOverallRubricScore() != null) scores.add(softSkill.getOverallRubricScore());
        Double combinedRubricScore = scores.isEmpty() ? null
                : round(scores.stream().mapToDouble(Double::doubleValue).average().orElse(0));

        Boolean combinedFinalEligible = (technical != null && softSkill != null)
                ? (technical.isFinalEligible() && softSkill.isFinalEligible())
                : null;

        return CombinedEvaluationResponse.builder()
                .studentId(studentId)
                .studentName(fullName(student))
                .technical(technical)
                .softSkill(softSkill)
                .combinedRubricScore(combinedRubricScore)
                .combinedFinalEligible(combinedFinalEligible)
                .hasTechnical(technical != null)
                .hasSoftSkill(softSkill != null)
                .build();
    }

    // ---------- Admin: rubric configuration ----------

    public EvaluationRubricConfigResponse getRubricConfig(TrainerType trainerType) {
        Optional<EvaluationRubricConfig> config = rubricConfigRepository.findByTrainerType(trainerType);
        return EvaluationRubricConfigResponse.builder()
                .trainerType(trainerType)
                .skills(config.map(EvaluationRubricConfig::getSkills).orElse(defaultRubricFor(trainerType)))
                .customized(config.isPresent())
                .updatedAt(config.map(EvaluationRubricConfig::getUpdatedAt).orElse(null))
                .updatedByAdminName(config.map(EvaluationRubricConfig::getUpdatedByAdminName).orElse(null))
                .build();
    }

    public List<EvaluationRubricConfigResponse> getAllRubricConfigs() {
        return List.of(getRubricConfig(TrainerType.TECHNICAL), getRubricConfig(TrainerType.SOFT_SKILL));
    }

    public EvaluationRubricConfigResponse updateRubricConfig(String adminEmail, TrainerType trainerType, UpdateEvaluationRubricRequest request) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (admin.getRole() != Role.ADMIN) {
            throw new BadRequestException("Only an admin can configure evaluation rubrics");
        }

        List<String> skills = request.getSkills().stream().distinct().toList();
        if (skills.size() < 3) {
            throw new BadRequestException("A rubric needs at least 3 distinct skills");
        }

        EvaluationRubricConfig config = rubricConfigRepository.findByTrainerType(trainerType)
                .orElse(EvaluationRubricConfig.builder().trainerType(trainerType).build());
        config.setSkills(skills);
        config.setUpdatedByAdminId(admin.getId());
        config.setUpdatedByAdminName(fullName(admin));
        config.setUpdatedAt(Instant.now());
        config = rubricConfigRepository.save(config);

        auditLogService.log(admin.getId(), admin.getEmail(), admin.getRole().name(),
                "EVALUATION_RUBRIC_UPDATED",
                "Updated the " + trainerType + " evaluation rubric to: " + String.join(", ", skills),
                null);

        return EvaluationRubricConfigResponse.builder()
                .trainerType(trainerType)
                .skills(config.getSkills())
                .customized(true)
                .updatedAt(config.getUpdatedAt())
                .updatedByAdminName(config.getUpdatedByAdminName())
                .build();
    }

    private List<String> getRubricSkills(TrainerType trainerType) {
        return rubricConfigRepository.findByTrainerType(trainerType)
                .map(EvaluationRubricConfig::getSkills)
                .orElseGet(() -> defaultRubricFor(trainerType));
    }

    private List<String> defaultRubricFor(TrainerType trainerType) {
        return trainerType == TrainerType.SOFT_SKILL ? DEFAULT_SOFT_SKILL_RUBRIC : DEFAULT_TECHNICAL_RUBRIC;
    }

    // ---------- Internal helpers ----------

    private void validateSkillScores(TrainerType trainerType, Map<String, Integer> skillScores) {
        List<String> expectedSkills = getRubricSkills(trainerType);
        for (String skill : skillScores.keySet()) {
            if (!expectedSkills.contains(skill)) {
                throw new BadRequestException("Unknown rubric skill for a " + trainerType + " trainer: " + skill);
            }
        }
        for (Integer score : skillScores.values()) {
            if (score == null || score < 0 || score > 10) {
                throw new BadRequestException("Each rubric score must be between 0 and 10");
            }
        }
    }

    private boolean resolveFinalEligible(Boolean override, String overrideReason, boolean systemEligible) {
        if (override != null && override != systemEligible && (overrideReason == null || overrideReason.isBlank())) {
            throw new BadRequestException("overrideReason is required when your eligibility decision differs from the system's");
        }
        return override != null ? override : systemEligible;
    }

    private double average(Map<String, Integer> skillScores) {
        return skillScores.values().stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    private void applyPlacementReadinessScore(User student, double overallRubricScore) {
        // Wired to the previously-unused placementReadinessScore field on User —
        // scaled to a 0-100 number so it's directly comparable to the other
        // percentage-based metrics elsewhere in the app.
        student.setPlacementReadinessScore(round(overallRubricScore * 10));
        student.setPlacementReadinessScoreUpdatedAt(Instant.now());
        userRepository.save(student);
    }

    private void notifyStudent(User student, String title, String message) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String senderRole = auth != null ? Role.TRAINER.name() : "SYSTEM";
        notificationService.sendToUser(student.getId(), title, message, senderRole);
    }

    private String trainerTypeLabel(TrainerType type) {
        return type == TrainerType.SOFT_SKILL ? "Soft Skill" : "Technical";
    }

    private Metrics computeMetrics(String studentId) {
        Metrics m = new Metrics();
        List<String> reasons = new ArrayList<>();

        // Raw numbers come from the shared StudentMetricsService (single source of
        // truth also used by the Placement module's eligibility engine) — this
        // method's own job is just applying this evaluation flow's thresholds.
        StudentRawMetrics raw = studentMetricsService.computeRawMetrics(studentId);
        m.attendancePercentage = raw.getAttendancePercentage();
        m.avgQuizPercentage = raw.getAvgQuizPercentage();
        m.avgCodingPercentage = raw.getAvgCodingPercentage();
        m.avgMockInterviewRating = raw.getAvgMockInterviewRating();

        // Eligibility: every tracked metric must exist AND clear its threshold.
        // A metric that's simply missing (e.g. no mock interviews yet) counts as not-eligible-yet,
        // with a clear reason, rather than being silently skipped.
        boolean eligible = true;

        if (m.attendancePercentage == null) {
            reasons.add("No attendance records yet");
            eligible = false;
        } else if (m.attendancePercentage < minAttendancePercentage) {
            reasons.add("Attendance " + m.attendancePercentage + "% is below the required " + minAttendancePercentage + "%");
            eligible = false;
        }

        if (m.avgQuizPercentage == null) {
            reasons.add("No quiz attempts yet");
            eligible = false;
        } else if (m.avgQuizPercentage < minQuizPercentage) {
            reasons.add("Average quiz score " + m.avgQuizPercentage + "% is below the required " + minQuizPercentage + "%");
            eligible = false;
        }

        if (m.avgCodingPercentage == null) {
            reasons.add("No coding submissions yet");
            eligible = false;
        } else if (m.avgCodingPercentage < minCodingPercentage) {
            reasons.add("Average coding score " + m.avgCodingPercentage + "% is below the required " + minCodingPercentage + "%");
            eligible = false;
        }

        if (m.avgMockInterviewRating == null) {
            reasons.add("No published mock interview evaluations yet");
            eligible = false;
        } else if (m.avgMockInterviewRating < minMockInterviewRating) {
            reasons.add("Average mock interview rating " + m.avgMockInterviewRating + " is below the required " + minMockInterviewRating);
            eligible = false;
        }

        m.systemEligible = eligible;
        m.reasons = reasons;
        return m;
    }

    private User getTrainer(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.TRAINER) {
            throw new BadRequestException("Only a trainer can perform this action");
        }
        return user;
    }

    private User getStudent(String studentId) {
        User user = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        if (user.getRole() != Role.STUDENT) {
            throw new BadRequestException("User " + studentId + " is not a student");
        }
        return user;
    }

    /**
     * Same ownership rule already enforced in getBatchOverview() — a trainer may only
     * act on a student who's in a batch they're actually assigned to (as either the
     * technical or soft-skill trainer). Applied here too so a trainer can't bypass the
     * batch-list screen by calling the metrics/submit endpoints directly with an
     * arbitrary studentId.
     */
    private void assertTrainerAssignedToStudent(User trainer, String studentId) {
        boolean assigned = batchRepository.findByStudentIdsContaining(studentId).stream()
                .anyMatch(batch -> trainer.getId().equals(batch.getTechnicalTrainerId())
                        || trainer.getId().equals(batch.getSoftSkillTrainerId()));
        if (!assigned) {
            throw new BadRequestException("You are not assigned to this student's batch");
        }
    }

    private String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }

    private StudentEvaluationResponse toResponse(StudentEvaluation e) {
        String studentName = userRepository.findById(e.getStudentId()).map(this::fullName).orElse(null);
        return StudentEvaluationResponse.builder()
                .id(e.getId())
                .studentId(e.getStudentId())
                .studentName(studentName)
                .batchId(e.getBatchId())
                .trainerId(e.getTrainerId())
                .trainerName(e.getTrainerName())
                .trainerType(e.getTrainerType())
                .attendancePercentage(e.getAttendancePercentage())
                .avgQuizPercentage(e.getAvgQuizPercentage())
                .avgCodingPercentage(e.getAvgCodingPercentage())
                .avgMockInterviewRating(e.getAvgMockInterviewRating())
                .systemEligible(e.isSystemEligible())
                .systemIneligibilityReasons(e.getSystemIneligibilityReasons())
                .skillScores(e.getSkillScores())
                .overallRubricScore(e.getOverallRubricScore())
                .remarks(e.getRemarks())
                .finalEligible(e.isFinalEligible())
                .overrideReason(e.getOverrideReason())
                .evaluatedAt(e.getEvaluatedAt())
                .edited(e.isEdited())
                .updatedAt(e.getUpdatedAt())
                .lastEditedBy(e.getLastEditedBy())
                .build();
    }

    private static Double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static class Metrics {
        Double attendancePercentage;
        Double avgQuizPercentage;
        Double avgCodingPercentage;
        Double avgMockInterviewRating;
        boolean systemEligible;
        List<String> reasons;
    }
}
