package com.infobeans.ibnextstep.assignment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentFileResponse {
    private String fileId;
    private String fileName;
    private long fileSizeBytes;
    private String mimeType;
    private String downloadUrl;
}
