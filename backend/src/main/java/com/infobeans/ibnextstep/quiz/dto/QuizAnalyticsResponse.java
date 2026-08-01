package com.infobeans.ibnextstep.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class QuizAnalyticsResponse {
    private int totalAssigned;
    private int attemptedCount;
    private int pendingCount;

    private double highestScore;
    private double lowestScore;
    private double averageScore;

    private double passPercentage;
    private double failPercentage;

    /** questionText -> number of students who got it correct, for a quick per-question difficulty read. */
    private Map<String, Long> questionWiseCorrectCount;

    private List<LeaderboardEntry> leaderboard;
    private List<String> weakStudentIds;
    private List<String> strongStudentIds;

    @Data
    @Builder
    @AllArgsConstructor
    public static class LeaderboardEntry {
        private String studentId;
        private double percentage;
        private long durationTakenSeconds;
    }
}
