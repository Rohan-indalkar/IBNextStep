package com.infobeans.ibnextstep.placement;

public enum PlacementStatus {
    /** Being configured by HR — not visible to students. */
    DRAFT,
    /** Live and visible to students; campus applications can be submitted. */
    PUBLISHED,
    /** Recruitment for this drive has ended — visible for history but no new applications. */
    CLOSED
}
