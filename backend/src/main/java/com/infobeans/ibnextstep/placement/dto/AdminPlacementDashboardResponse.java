package com.infobeans.ibnextstep.placement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPlacementDashboardResponse {

    private long totalStudents;
    private long activeStudents;
    private long placedStudents;
    private long unplacedStudents;
    private double placementPercentage;

    private long totalCompanies;
    private long activeCompanies;
    private long campusDrives;
    private long offCampusDrives;

    private long applications;
    private long selections;
    private long rejections;

    private Double highestPackageLpa;
    private Double averagePackageLpa;

    /** Placed-student count, keyed by department name. */
    private Map<String, Long> departmentWisePlacement;
    /** Selection count, keyed by company name. */
    private Map<String, Long> companyWisePlacement;
    /** Selection count, keyed by month label (e.g. "2026-06"). */
    private Map<String, Long> monthlyPlacementTrend;
    /** Selection count, keyed by year (e.g. "2026"). */
    private Map<String, Long> yearWisePlacement;
}
