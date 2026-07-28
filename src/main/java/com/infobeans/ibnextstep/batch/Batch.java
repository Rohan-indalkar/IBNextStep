package com.infobeans.ibnextstep.batch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "batches")
public class Batch {

    @Id
    private String id;

    private String name;
    private String courseId;

    private String technicalTrainerId;
    private String softSkillTrainerId;

    @Builder.Default
    private List<String> studentIds = List.of();

    private LocalDate startDate;
    private LocalDate endDate;

    private BatchStatus status;

    @Builder.Default
    private List<TimetableEntry> timetable = List.of();

    @CreatedDate
    private Instant createdAt;

    public enum BatchStatus {
        ACTIVE,
        INACTIVE
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimetableEntry {
        private LocalDate date;
        private String topic;
        private String trainerId;
    }
}
