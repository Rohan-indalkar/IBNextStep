package com.infobeans.ibnextstep.codingassessment;

import com.infobeans.ibnextstep.batch.Batch;
import com.infobeans.ibnextstep.batch.BatchRepository;
import com.infobeans.ibnextstep.codingassessment.dto.TrainerAnalyticsResponse;
import com.infobeans.ibnextstep.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssessmentAnalyticsService {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentSessionRepository sessionRepository;
    private final SubmissionRepository submissionRepository;
    private final BatchRepository batchRepository;

    private static final Set<SessionStatus> COMPLETED_STATUSES =
            Set.of(SessionStatus.SUBMITTED, SessionStatus.AUTO_SUBMITTED_TIMER, SessionStatus.AUTO_SUBMITTED_VIOLATION);

    public TrainerAnalyticsResponse analyze(String assessmentId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found: " + assessmentId));
        Batch batch = batchRepository.findById(assessment.getBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + assessment.getBatchId()));

        List<AssessmentSession> allSessions = sessionRepository.findByAssessmentId(assessmentId);
        List<AssessmentSession> completed = allSessions.stream()
                .filter(s -> COMPLETED_STATUSES.contains(s.getStatus()))
                .toList();

        int totalStudents = batch.getStudentIds().size();
        int submitted = completed.size();
        int pending = Math.max(totalStudents - submitted, 0);

        double highest = completed.stream().mapToDouble(AssessmentSession::getTotalMarksAwarded).max().orElse(0);
        double lowest = completed.stream().mapToDouble(AssessmentSession::getTotalMarksAwarded).min().orElse(0);
        double average = completed.stream().mapToDouble(AssessmentSession::getTotalMarksAwarded).average().orElse(0);

        long passedCount = completed.stream().filter(s -> s.getTotalMarksAwarded() >= assessment.getPassingMarks()).count();
        double passPct = submitted == 0 ? 0 : (passedCount * 100.0) / submitted;
        double failPct = submitted == 0 ? 0 : 100.0 - passPct;

        List<Submission> allSubmissions = submissionRepository.findByAssessmentIdAndRunOnlyFalse(assessmentId);
        Map<String, Long> questionWiseAccepted = allSubmissions.stream()
                .filter(s -> s.getStatus() == SubmissionStatus.ACCEPTED)
                .collect(Collectors.groupingBy(Submission::getQuestionId, Collectors.counting()));

        List<TrainerAnalyticsResponse.LeaderboardEntry> leaderboard = completed.stream()
                .sorted(Comparator.comparingDouble(AssessmentSession::getTotalMarksAwarded).reversed()
                        .thenComparing(s -> s.getSubmittedAt() == null ? Long.MAX_VALUE : s.getSubmittedAt().toEpochMilli()))
                .map(s -> TrainerAnalyticsResponse.LeaderboardEntry.builder()
                        .studentId(s.getStudentId())
                        .totalMarks(s.getTotalMarksAwarded())
                        .submittedAtEpoch(s.getSubmittedAt() == null ? 0 : s.getSubmittedAt().toEpochMilli())
                        .build())
                .toList();

        return TrainerAnalyticsResponse.builder()
                .totalStudents(totalStudents)
                .submittedCount(submitted)
                .pendingCount(pending)
                .highestScore(highest)
                .lowestScore(lowest)
                .averageScore(average)
                .passPercentage(passPct)
                .failPercentage(failPct)
                .questionWiseAcceptedCount(questionWiseAccepted)
                .leaderboard(leaderboard)
                .build();
    }

    public List<AssessmentSession> allSessions(String assessmentId) {
        return sessionRepository.findByAssessmentId(assessmentId);
    }

    public List<Submission> allSubmissions(String assessmentId) {
        return submissionRepository.findByAssessmentIdAndRunOnlyFalse(assessmentId);
    }
}
