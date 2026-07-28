package com.infobeans.ibnextstep.batch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TimetableEntryRequest {
    @NotNull
    private LocalDate date;

    @NotBlank
    private String topic;

    @NotBlank
    private String trainerId;
}
