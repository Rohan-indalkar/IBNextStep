package com.infobeans.ibnextstep.placement.dto;

import com.infobeans.ibnextstep.placement.PlacementApplicationStatus;
import com.infobeans.ibnextstep.placement.PlacementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentPlacementResponse {
    private String id;
    private String companyName;
    private String companyLogoPath;
    private String title;
    private String description;
    private PlacementType type;
    private Instant applicationDeadline;
    private String pdfFileName;
    private String externalApplyLink;
    private Double packageLpa;

    /** Always true, with no failed criteria, for OFF_CAMPUS drives. */
    private boolean eligible;
    private EligibilityCheckResponse eligibility;

    private boolean applyEnabled;
    private boolean pdfDownloadEnabled;

    private boolean alreadyApplied;
    private PlacementApplicationStatus myApplicationStatus;
}
