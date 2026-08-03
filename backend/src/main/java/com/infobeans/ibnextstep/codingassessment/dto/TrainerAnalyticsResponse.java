package com.infobeans.ibnextstep.codingassessment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class TrainerAnalyticsResponse {
    private int totalStudents;
    private int submittedCount;
    private int pendingCount;

    private double highestScore;
    private double lowestScore;
    private double averageScore;

    private double passPercentage;
    private double failPercentage;

    /** questionId -> number of students who got Accepted on that question. */
    private Map<String, Long> questionWiseAcceptedCount;

    private List<LeaderboardEntry> leaderboard;

    @Data
    @Builder
    @AllArgsConstructor
    public static class LeaderboardEntry {
        private String studentId;
        private double totalMarks;
        private long submittedAtEpoch;
    }
}
