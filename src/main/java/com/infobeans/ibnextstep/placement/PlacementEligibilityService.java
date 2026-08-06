package com.infobeans.ibnextstep.placement;

import com.infobeans.ibnextstep.placement.dto.EligibilityCheckResponse;
import com.infobeans.ibnextstep.resume.Resume;
import com.infobeans.ibnextstep.resume.ResumeRepository;
import com.infobeans.ibnextstep.resume.ResumeStatus;
import com.infobeans.ibnextstep.studentevaluation.StudentEvaluation;
import com.infobeans.ibnextstep.studentevaluation.StudentEvaluationRepository;
import com.infobeans.ibnextstep.studentevaluation.StudentMetricsService;
import com.infobeans.ibnextstep.studentevaluation.StudentRawMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Evaluates a student against a campus placement's {@link Placement.EligibilityCriteria}.
 * <p>
 * Reuses the exact same numbers as the rest of the platform rather than
 * recomputing anything: system metrics come from {@link StudentMetricsService}
 * (the same source {@code StudentEvaluationService} uses for trainer
 * evaluations), the "Student Evaluation" criterion reads the student's latest
 * {@link StudentEvaluation} record, and resume completion reads {@link Resume}.
 * <p>
 * Every configured rule is checked — a student's response always lists every
 * rule they fail, not just the first one, per the required student-facing flow.
 */
@Service
@RequiredArgsConstructor
public class PlacementEligibilityService {

    private final StudentMetricsService studentMetricsService;
    private final StudentEvaluationRepository studentEvaluationRepository;
    private final ResumeRepository resumeRepository;

    public EligibilityCheckResponse check(String studentId, Placement.EligibilityCriteria criteria) {
        List<EligibilityCheckResponse.FailedCriterion> failures = new ArrayList<>();

        // Off-campus (or a campus drive somehow created without criteria) has nothing to gate on.
        if (criteria == null) {
            return EligibilityCheckResponse.builder().eligible(true).build();
        }

        StudentRawMetrics metrics = studentMetricsService.computeRawMetrics(studentId);

        checkPercentage(failures, "Attendance", criteria.getMinAttendancePercentage(), metrics.getAttendancePercentage(), "%");
        checkPercentage(failures, "Quiz Score", criteria.getMinQuizPercentage(), metrics.getAvgQuizPercentage(), "%");
        checkPercentage(failures, "Coding Assessment Score", criteria.getMinCodingPercentage(), metrics.getAvgCodingPercentage(), "%");
        checkPercentage(failures, "Mock Interview Score", criteria.getMinMockInterviewRating(), metrics.getAvgMockInterviewRating(), "");

        if (criteria.getMinStudentEvaluationScore() != null) {
            Optional<StudentEvaluation> latest = studentEvaluationRepository.findTopByStudentIdOrderByEvaluatedAtDesc(studentId);
            Double score = latest.map(StudentEvaluation::getOverallRubricScore).orElse(null);
            if (score == null) {
                failures.add(criterion("Student Evaluation", String.valueOf(criteria.getMinStudentEvaluationScore()), "None",
                        "No student evaluation submitted yet"));
            } else if (score < criteria.getMinStudentEvaluationScore()) {
                failures.add(criterion("Student Evaluation", String.valueOf(criteria.getMinStudentEvaluationScore()), String.valueOf(score),
                        "Student evaluation score " + score + " is below the required " + criteria.getMinStudentEvaluationScore()));
            }
        }

        if (Boolean.TRUE.equals(criteria.getRequireResumeApproved())) {
            Optional<Resume> resume = resumeRepository.findByStudentId(studentId);
            ResumeStatus status = resume.map(Resume::getCurrentStatus).orElse(null);
            if (status != ResumeStatus.APPROVED) {
                failures.add(criterion("Resume", "Approved", status == null ? "Not submitted" : status.name(),
                        "Resume must be approved before applying"));
            }
        }

        return EligibilityCheckResponse.builder()
                .eligible(failures.isEmpty())
                .failedCriteria(failures)
                .build();
    }

    private void checkPercentage(List<EligibilityCheckResponse.FailedCriterion> failures, String label,
                                  Double required, Double current, String unit) {
        if (required == null) {
            return; // HR didn't configure this rule for this drive
        }
        if (current == null) {
            failures.add(criterion(label, required + unit, "None", "No " + label.toLowerCase() + " data yet"));
            return;
        }
        if (current < required) {
            failures.add(criterion(label, required + unit, current + unit,
                    label + " required " + required + unit + ", current " + current + unit));
        }
    }

    private EligibilityCheckResponse.FailedCriterion criterion(String name, String required, String current, String reason) {
        return EligibilityCheckResponse.FailedCriterion.builder()
                .criterion(name)
                .required(required)
                .current(current)
                .reason(reason)
                .build();
    }
}
