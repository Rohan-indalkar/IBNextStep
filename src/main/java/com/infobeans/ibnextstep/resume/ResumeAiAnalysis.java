package com.infobeans.ibnextstep.resume;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Structured feedback Gemini returns for one resume version. Stored inline
 * on {@link Resume.ResumeVersion} so re-opening the same version doesn't
 * trigger another Gemini call — only a re-upload (new version) or an
 * explicit "refresh" invalidates it.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeAiAnalysis {

    /** 0-100. Null if the model didn't return a parseable score. */
    private Integer overallScore;

    private String summary;

    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> missingSections;
    private List<String> atsIssues;
    private List<String> suggestions;

    /** Ready-to-edit paragraph a trainer can drop straight into ReviewResumeRequest.suggestions. */
    private String suggestedReviewText;

    private Instant analyzedAt;
}
