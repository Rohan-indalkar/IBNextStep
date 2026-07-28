package com.infobeans.ibnextstep.dashboard;

import com.infobeans.ibnextstep.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {

    private final ReportExportService reportExportService;
    private final AdminDashboardService adminDashboardService;

    /** "View System Reports" — high-level counts, reusing the dashboard stats. */
    @GetMapping("/summary")
    public ApiResponse<AdminDashboardStats> summary() {
        return ApiResponse.success(adminDashboardService.getStats());
    }

    /** "Search / Filter Reports" — lists the available reports, optionally filtered by keyword. */
    @GetMapping
    public ApiResponse<List<ReportInfo>> search(@RequestParam(required = false) String query) {
        return ApiResponse.success(reportExportService.availableReports(query));
    }

    @GetMapping("/users/export")
    public ResponseEntity<byte[]> exportUsers() {
        byte[] excelBytes = reportExportService.exportUsersToExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users_report.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelBytes);
    }

    /** "Export PDF/Excel" — PDF variant of the user report. */
    @GetMapping("/users/export/pdf")
    public ResponseEntity<byte[]> exportUsersPdf() {
        byte[] pdfBytes = reportExportService.exportUsersToPdf();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users_report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
