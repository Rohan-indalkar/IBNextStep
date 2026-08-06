package com.infobeans.ibnextstep.placement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * A single recruitment drive run by HR against one {@link Company}. Campus
 * drives carry a configurable {@link EligibilityCriteria} that gates the
 * apply flow; off-campus drives never do (every student can view/download/
 * open the external link, no internal application is created).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "placements")
public class Placement {

    @Id
    private String id;

    @Indexed
    private String companyId;
    /** Denormalized for fast list rendering — kept in sync by PlacementService whenever the company name changes. */
    private String companyName;
    private String companyLogoPath;

    private String title;
    private String description;

    @Indexed
    private PlacementType type;

    @Indexed
    private PlacementStatus status;

    /** Null for OFF_CAMPUS drives — they never gate on eligibility. */
    private EligibilityCriteria eligibility;

    private Instant applicationDeadline;

    /** Relative path from FileStorageService — the placement's JD/brochure PDF. */
    private String pdfPath;
    private String pdfFileName;

    /** Off-campus "Apply" destination. Campus drives may also carry one as a supplementary link. */
    private String externalApplyLink;

    /** Ordered round template HR configures at creation time — seeded onto each application when HR starts scheduling interviews. */
    @Builder.Default
    private List<RoundTemplate> interviewRoundTemplates = List.of();

    /** CTC offered, in LPA — optional, used for admin's highest/average package analytics. */
    private Double packageLpa;

    private String createdByHrId;
    private String createdByHrName;

    private Instant publishedAt;
    private Instant closedAt;

    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoundTemplate {
        private int roundNumber;
        /** e.g. "Online Assessment", "Technical Interview", "Managerial Interview", "HR Interview". */
        private String name;
    }

    /**
     * Every field is nullable/optional — HR configures only the rules that
     * apply to a given drive, and only configured (non-null) rules are
     * checked. This intentionally does not include CGPA / Branch / Passing
     * Year / Backlogs: the platform doesn't track student academic-profile
     * data yet, so those rules aren't offered until that data exists.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EligibilityCriteria {
        private Double minAttendancePercentage;
        private Double minQuizPercentage;
        private Double minCodingPercentage;
        private Double minMockInterviewRating;
        /** Compared against the student's latest StudentEvaluation.overallRubricScore (0-10 scale). */
        private Double minStudentEvaluationScore;
        /** If true, the student's resume must be in APPROVED status to be eligible. */
        private Boolean requireResumeApproved;
    }
}
