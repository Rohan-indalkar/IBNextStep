package com.infobeans.ibnextstep.attendance;

import com.infobeans.ibnextstep.attendance.dto.AttendanceRecordResponse;
import com.infobeans.ibnextstep.attendance.dto.StudentMonthlyAttendanceResponse;
import com.infobeans.ibnextstep.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/student/attendance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentAttendanceController {

    private final AttendanceService attendanceService;
    private final AttendanceReportService attendanceReportService;

    /** "View Attendance Record" — view only, no edit endpoint exists on this controller. */
    @GetMapping("/records")
    public ApiResponse<List<AttendanceRecordResponse>> getMyRecord(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication auth) {
        return ApiResponse.success(attendanceService.getMyAttendanceRecord(auth.getName(), from, to));
    }

    /** "View Monthly Attendance" + "View Attendance Percentage". */
    @GetMapping("/monthly")
    public ApiResponse<StudentMonthlyAttendanceResponse> getMyMonthlyAttendance(
            @RequestParam int year,
            @RequestParam int month,
            Authentication auth) {
        return ApiResponse.success(attendanceService.getMyMonthlyAttendance(auth.getName(), year, month));
    }

    /** "Download Attendance Report (Optional)". */
    @GetMapping("/monthly/export")
    public ResponseEntity<byte[]> downloadMyMonthlyReport(
            @RequestParam int year,
            @RequestParam int month,
            Authentication auth) {
        byte[] excelBytes = attendanceReportService.exportMyMonthlyReport(auth.getName(), year, month);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=attendance_" + year + "_" + month + ".xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelBytes);
    }
}
