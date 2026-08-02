package com.infobeans.ibnextstep.attendance.dto;

import com.infobeans.ibnextstep.attendance.AttendanceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAttendanceEntry {

    @NotBlank
    private String studentId;

    @NotNull
    private AttendanceStatus status;

    private String remarks;
}
