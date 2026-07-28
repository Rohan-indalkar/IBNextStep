package com.infobeans.ibnextstep.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkImportResult {
    private int totalRows;
    private int successCount;
    private int failureCount;
    private List<RowError> errors = new ArrayList<>();

    public void recordSuccess() {
        successCount++;
    }

    public void recordFailure(int rowNumber, String reason) {
        failureCount++;
        errors.add(new RowError(rowNumber, reason));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RowError {
        private int rowNumber;
        private String reason;
    }
}
