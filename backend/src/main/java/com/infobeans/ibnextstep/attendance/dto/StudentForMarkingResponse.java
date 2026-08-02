package com.infobeans.ibnextstep.attendance.dto;

import com.infobeans.ibnextstep.attendance.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One row of "View student list" for a given batch + date. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentForMarkingResponse {
    private String studentId;
    private String firstName;
    private String lastName;
    private String email;

    /** Null if not marked yet for this date. */
    private AttendanceStatus status;
    private String remarks;
}
