package com.infobeans.ibnextstep.placement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrDashboardResponse {

    // ---------- Cards ----------
    private long totalCompanies;
    private long activeCompanies;
    private long activePlacementDrives;
    private long campusDrives;
    private long offCampusDrives;
    private long applicationsReceived;
    private long shortlisted;
    private long interviewScheduled;
    private long selected;
    private long rejected;
    private long todaysInterviews;
    private long upcomingInterviews;

    // ---------- Charts ----------
    /** Applications received, keyed by month label (e.g. "2026-06"). */
    private Map<String, Long> placementTrend;
    /** Selections, keyed by company name. */
    private Map<String, Long> companyWiseHiring;
    /** Applications, keyed by department name. */
    private Map<String, Long> departmentWiseApplications;
    /** Selections, keyed by round type name. */
    private Map<String, Long> roundWiseSelection;

    private List<UpcomingInterviewDto> nextUpcomingInterviews;
}
