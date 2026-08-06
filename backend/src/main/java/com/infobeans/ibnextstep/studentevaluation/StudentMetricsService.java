package com.infobeans.ibnextstep.studentevaluation;

import com.infobeans.ibnextstep.attendance.AttendanceRecord;
import com.infobeans.ibnextstep.attendance.AttendanceRecordRepository;
import com.infobeans.ibnextstep.attendance.AttendanceStatus;
import com.infobeans.ibnextstep.codingassessment.CodingQuestionRepository;
import com.infobeans.ibnextstep.codingassessment.Submission;
import com.infobeans.ibnextstep.codingassessment.SubmissionRepository;
import com.infobeans.ibnextstep.mockinterview.MockInterview;
import com.infobeans.ibnextstep.mockinterview.MockInterviewRepository;
import com.infobeans.ibnextstep.mockinterview.MockInterviewStatus;
import com.infobeans.ibnextstep.quiz.QuizResult;
import com.infobeans.ibnextstep.quiz.QuizResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Single source of truth for pulling a student's live metrics out of the
 * attendance, quiz, coding assessment and mock interview modules.
 * <p>
 * This computation originally lived, threshold-and-all, inside
 * {@link StudentEvaluationService}. It has been extracted here (metrics
 * computation only — no threshold/eligibility logic) so the Placement module
 * can reuse the exact same numbers instead of re-querying and re-aggregating
 * those four modules itself. {@link StudentEvaluationService} now delegates
 * to this class too, so behaviour there is unchanged.
 */
@Service
@RequiredArgsConstructor
public class StudentMetricsService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final QuizResultRepository quizResultRepository;
    private final SubmissionRepository submissionRepository;
    private final CodingQuestionRepository codingQuestionRepository;
    private final MockInterviewRepository mockInterviewRepository;

    public StudentRawMetrics computeRawMetrics(String studentId) {
        StudentRawMetrics.StudentRawMetricsBuilder builder = StudentRawMetrics.builder();

        // Attendance: PRESENT and LATE both count as attended; ABSENT does not.
        List<AttendanceRecord> attendanceRecords = attendanceRecordRepository.findByStudentId(studentId);
        if (!attendanceRecords.isEmpty()) {
            long attended = attendanceRecords.stream()
                    .filter(r -> r.getStatus() == AttendanceStatus.PRESENT || r.getStatus() == AttendanceStatus.LATE)
                    .count();
            builder.attendancePercentage(round(attended * 100.0 / attendanceRecords.size()));
        }

        // Quiz: average of each attempt's already-computed percentage.
        List<QuizResult> quizResults = quizResultRepository.findByStudentId(studentId);
        if (!quizResults.isEmpty()) {
            builder.avgQuizPercentage(round(quizResults.stream().mapToDouble(QuizResult::getPercentage).average().orElse(0)));
        }

        // Coding: best submission per question (not every attempt — resubmits shouldn't drag the average down),
        // as a percentage of that question's max marks (questions carry different weights).
        List<Submission> submissions = submissionRepository.findByStudentIdAndRunOnlyFalse(studentId);
        if (!submissions.isEmpty()) {
            Map<String, Double> bestMarksByQuestion = new HashMap<>();
            for (Submission s : submissions) {
                bestMarksByQuestion.merge(s.getQuestionId(), s.getMarksAwarded(), Math::max);
            }
            List<Double> percentages = new ArrayList<>();
            for (Map.Entry<String, Double> entry : bestMarksByQuestion.entrySet()) {
                codingQuestionRepository.findById(entry.getKey()).ifPresent(question -> {
                    if (question.getMarks() > 0) {
                        percentages.add(entry.getValue() * 100.0 / question.getMarks());
                    }
                });
            }
            if (!percentages.isEmpty()) {
                builder.avgCodingPercentage(round(percentages.stream().mapToDouble(Double::doubleValue).average().orElse(0)));
            }
        }

        // Mock interviews: only ones the trainer has actually published a rating for.
        List<MockInterview> publishedInterviews = mockInterviewRepository.findByStudentIdAndStatus(studentId, MockInterviewStatus.PUBLISHED);
        List<Double> ratings = publishedInterviews.stream()
                .map(MockInterview::getEvaluation)
                .filter(e -> e != null && e.getOverallRating() != null)
                .map(MockInterview.Evaluation::getOverallRating)
                .toList();
        if (!ratings.isEmpty()) {
            builder.avgMockInterviewRating(round(ratings.stream().mapToDouble(Double::doubleValue).average().orElse(0)));
        }

        return builder.build();
    }

    private static Double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
