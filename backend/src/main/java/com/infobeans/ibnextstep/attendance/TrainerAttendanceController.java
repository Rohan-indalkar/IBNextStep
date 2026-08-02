package com.infobeans.ibnextstep.attendance;

import com.infobeans.ibnextstep.attendance.dto.DailyAttendanceSummaryResponse;
import com.infobeans.ibnextstep.attendance.dto.MarkAttendanceRequest;
import com.infobeans.ibnextstep.attendance.dto.MonthlyBatchAttendanceResponse;
import com.infobeans.ibnextstep.attendance.dto.StudentForMarkingResponse;
import com.infobeans.ibnextstep.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/trainer/attendance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TRAINER')")
public class TrainerAttendanceController {

    private final AttendanceService attendanceService;

    /** "Select Batch" -> "Select Date" -> "View Student List". */
    @GetMapping("/batches/{batchId}/students")
    public ApiResponse<List<StudentForMarkingResponse>> getStudentList(
            @PathVariable String batchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication auth) {
        return ApiResponse.success(attendanceService.getStudentListForMarking(auth.getName(), batchId, date));
    }

    /** "Search student" on the marking screen. */
    @GetMapping("/batches/{batchId}/students/search")
    public ApiResponse<List<StudentForMarkingResponse>> searchStudent(
            @PathVariable String batchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String query,
            Authentication auth) {
        return ApiResponse.success(attendanceService.searchStudentInBatch(auth.getName(), batchId, date, query));
    }

    /** "Mark Attendance" -> "Update Attendance" -> "Save Attendance". */
    @PostMapping("/mark")
    public ApiResponse<Void> markAttendance(@Valid @RequestBody MarkAttendanceRequest request, Authentication auth) {
        attendanceService.markAttendance(auth.getName(), request);
        return ApiResponse.success("Attendance updated successfully", null);
    }

    /** "Edit attendance of previous dates (if permitted)". */
    @PutMapping("/edit")
    public ApiResponse<Void> editAttendance(@Valid @RequestBody MarkAttendanceRequest request, Authentication auth) {
        attendanceService.editAttendance(auth.getName(), request);
        return ApiResponse.success("Attendance updated successfully", null);
    }

    /** "View daily attendance summary" (also serves as "Filter by batch"). */
    @GetMapping("/batches/{batchId}/summary/daily")
    public ApiResponse<DailyAttendanceSummaryResponse> dailySummary(
            @PathVariable String batchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication auth) {
        return ApiResponse.success(attendanceService.getDailySummary(auth.getName(), batchId, date));
    }

    /** "View monthly attendance summary". */
    @GetMapping("/batches/{batchId}/summary/monthly")
    public ApiResponse<MonthlyBatchAttendanceResponse> monthlySummary(
            @PathVariable String batchId,
            @RequestParam int year,
            @RequestParam int month,
            Authentication auth) {
        return ApiResponse.success(attendanceService.getMonthlySummary(auth.getName(), batchId, year, month));
    }
}
