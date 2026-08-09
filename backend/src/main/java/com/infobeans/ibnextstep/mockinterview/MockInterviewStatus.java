package com.infobeans.ibnextstep.mockinterview;

public enum MockInterviewStatus {
    /** Created, student notified, waiting for the scheduled time. */
    SCHEDULED,
    /** Trainer has conducted the session but not yet filled the evaluation form. */
    CONDUCTED,
    /** Evaluation form submitted, scores calculated — not yet visible to the student. */
    EVALUATED,
    /** Evaluation report published — visible to the student, dashboard/readiness score updated. */
    PUBLISHED,
    /** Cancelled before it happened. */
    CANCELLED
}
