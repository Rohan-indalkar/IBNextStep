package com.infobeans.ibnextstep.mockinterview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Backs "Update Student Performance Dashboard" / "Generate Analytics & Reports"
 * from the flow — computed live from published mock_interviews records rather
 * than a separately materialized dashboard table, so there's nothing to keep
 * in sync.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockInterviewAnalyticsResponse {
    private long totalScheduled;
    private long totalConducted;
    private long totalEvaluated;
    private long totalPublished;
    private long totalCancelled;

    /** Only counts PUBLISHED interviews. */
    private Double averageOverallRating;
    private Map<String, Double> averageRatingByInterviewType;
    private Map<String, Double> averageScoreByParameter;
}
