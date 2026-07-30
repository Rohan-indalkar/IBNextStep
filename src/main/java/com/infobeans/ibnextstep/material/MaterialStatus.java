package com.infobeans.ibnextstep.material;

public enum MaterialStatus {
    /** Saved but not visible to any student yet. */
    DRAFT,
    /** Will flip to PUBLISHED automatically once scheduledAt passes (see MaterialPublishScheduler). */
    SCHEDULED,
    /** Live — visible to students in assigned batches, subject to visibleFrom / expiryDate. */
    PUBLISHED
}
