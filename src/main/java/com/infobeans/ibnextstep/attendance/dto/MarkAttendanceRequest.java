package com.infobeans.ibnextstep.attendance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/** Body for "Save attendance" — one batch, one date, every student's mark. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkAttendanceRequest {

    @NotBlank
    private String batchId;

    @NotNull
    private LocalDate date;

    @NotEmpty
    @Valid
    private List<StudentAttendanceEntry> entries;
}
