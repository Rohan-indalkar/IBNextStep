package com.infobeans.ibnextstep.quiz;

import com.infobeans.ibnextstep.batch.Batch;
import com.infobeans.ibnextstep.batch.BatchRepository;
import com.infobeans.ibnextstep.common.exception.ResourceNotFoundException;
import com.infobeans.ibnextstep.quiz.dto.QuizAnalyticsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizAnalyticsService {

    private final QuizRepository quizRepository;
    private final QuizResultRepository resultRepository;
    private final QuizAnswerRepository answerRepository;
    private final BatchRepository batchRepository;

    private static final double WEAK_THRESHOLD = 40.0;
    private static final double STRONG_THRESHOLD = 80.0;

    public QuizAnalyticsResponse analyze(String quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found: " + quizId));
        Batch batch = batchRepository.findById(quiz.getBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + quiz.getBatchId()));

        List<QuizResult> results = resultRepository.findByQuizId(quizId);
        int totalAssigned = batch.getStudentIds().size();
        int attempted = results.size();
        int pending = totalAssigned - attempted;

        double highest = results.stream().mapToDouble(QuizResult::getPercentage).max().orElse(0);
        double lowest = results.stream().mapToDouble(QuizResult::getPercentage).min().orElse(0);
        double average = results.stream().mapToDouble(QuizResult::getPercentage).average().orElse(0);

        long passedCount = results.stream().filter(QuizResult::isPassed).count();
        double passPct = attempted == 0 ? 0 : (passedCount * 100.0) / attempted;
        double failPct = attempted == 0 ? 0 : 100.0 - passPct;

        List<QuizAnalyticsResponse.LeaderboardEntry> leaderboard = results.stream()
                .sorted(Comparator.comparingDouble(QuizResult::getPercentage).reversed()
                        .thenComparingLong(QuizResult::getDurationTakenSeconds))
                .map(r -> QuizAnalyticsResponse.LeaderboardEntry.builder()
                        .studentId(r.getStudentId())
                        .percentage(r.getPercentage())
                        .durationTakenSeconds(r.getDurationTakenSeconds())
                        .build())
                .toList();

        List<String> weak = results.stream().filter(r -> r.getPercentage() < WEAK_THRESHOLD).map(QuizResult::getStudentId).toList();
        List<String> strong = results.stream().filter(r -> r.getPercentage() >= STRONG_THRESHOLD).map(QuizResult::getStudentId).toList();

        List<QuizAnswer> allAnswers = answerRepository.findByQuizId(quizId);
        Map<String, Long> questionWiseCorrectCount = allAnswers.stream()
                .filter(QuizAnswer::isCorrect)
                .collect(Collectors.groupingBy(QuizAnswer::getQuestionText, Collectors.counting()));

        return QuizAnalyticsResponse.builder()
                .totalAssigned(totalAssigned)
                .attemptedCount(attempted)
                .pendingCount(Math.max(pending, 0))
                .highestScore(highest)
                .lowestScore(lowest)
                .averageScore(average)
                .passPercentage(passPct)
                .failPercentage(failPct)
                .questionWiseCorrectCount(questionWiseCorrectCount)
                .leaderboard(leaderboard)
                .weakStudentIds(weak)
                .strongStudentIds(strong)
                .build();
    }

    public List<QuizResult> results(String quizId) {
        return resultRepository.findByQuizId(quizId);
    }
}
