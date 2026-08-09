package com.infobeans.ibnextstep.course.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AssignSkillsRequest {
    @NotEmpty
    private List<String> skillIds;
}
