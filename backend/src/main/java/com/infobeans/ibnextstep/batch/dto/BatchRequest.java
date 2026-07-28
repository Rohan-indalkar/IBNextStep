package com.infobeans.ibnextstep.batch.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BatchRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String courseId;

    private LocalDate startDate;
    private LocalDate endDate;
}
