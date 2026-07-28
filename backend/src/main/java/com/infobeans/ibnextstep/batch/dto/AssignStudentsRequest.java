package com.infobeans.ibnextstep.batch.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AssignStudentsRequest {
    @NotEmpty
    private List<String> studentIds;
}
