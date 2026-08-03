package com.infobeans.ibnextstep.codingassessment.dto;

import com.infobeans.ibnextstep.codingassessment.ProgrammingLanguage;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SaveDraftRequest {
    @NotNull
    private ProgrammingLanguage language;

    /** Can be blank/empty — a student might save an empty draft as a "clear" action. */
    private String code = "";
}
