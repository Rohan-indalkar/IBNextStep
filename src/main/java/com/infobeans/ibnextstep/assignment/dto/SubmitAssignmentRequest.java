package com.infobeans.ibnextstep.assignment.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Sent as the "data" part of a multipart request; files (if any) go in the "files" part, matched by questionId order or as general attachments. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAssignmentRequest {

    @NotEmpty(message = "Provide at least one answer (text and/or an uploaded file)")
    private List<AnswerRequest> answers;
}
