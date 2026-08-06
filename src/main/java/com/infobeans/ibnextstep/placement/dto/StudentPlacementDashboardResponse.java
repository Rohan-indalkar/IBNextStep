package com.infobeans.ibnextstep.placement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentPlacementDashboardResponse {

    private long appliedCount;
    private long selectedCount;
    private long rejectedCount;
    private long campusOpenCount;
    private long offCampusOpenCount;

    private List<UpcomingInterviewDto> upcomingInterviews;
    private List<InterviewHistoryEntry> interviewHistory;
    private List<PlacementApplicationResponse> applications;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterviewHistoryEntry {
        private String placementTitle;
        private String companyName;
        private int roundNumber;
        private String roundType;
        private String result;
        private java.time.Instant completedAt;
    }
}
