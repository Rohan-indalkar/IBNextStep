package com.infobeans.ibnextstep.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

/**
 * One record per (batch, student, date). Marked/edited by a trainer,
 * read-only for the student it belongs to.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "attendance_records")
@CompoundIndex(name = "batch_student_date_idx", def = "{'batchId': 1, 'studentId': 1, 'date': 1}", unique = true)
public class AttendanceRecord {

    @Id
    private String id;

    private String batchId;
    private String studentId;

    /** Trainer who last created/updated this entry. */
    private String markedByTrainerId;

    private LocalDate date;
    private AttendanceStatus status;

    private String remarks;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
