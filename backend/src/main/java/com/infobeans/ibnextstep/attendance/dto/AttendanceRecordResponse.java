package com.infobeans.ibnextstep.attendance.dto;

import com.infobeans.ibnextstep.attendance.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecordResponse {
    private LocalDate date;
    private AttendanceStatus status;
    private String batchId;
}
