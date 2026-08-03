package com.infobeans.ibnextstep.codingassessment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "assessment")
public class Assessment {

    @Id
    private String id;

    private String title;
    private String description;
    private String batchId;

    private int durationMinutes;
    private Instant startTime;
    private Instant endTime;

    private double passingMarks;
    private int maxAttempts;
    private List<ProgrammingLanguage> allowedLanguages;

    private AssessmentStatus status;

    /** Ordered references into the CodingQuestion collection — questions live as their own top-level documents, not embedded. */
    private List<String> questionIds;

    private String trainerId;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant publishedAt;
}
