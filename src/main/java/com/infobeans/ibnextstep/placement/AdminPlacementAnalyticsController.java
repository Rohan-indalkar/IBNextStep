package com.infobeans.ibnextstep.placement;

import com.infobeans.ibnextstep.common.ApiResponse;
import com.infobeans.ibnextstep.placement.dto.AdminPlacementDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin is a system administrator with zero placement-operation authority —
 * this controller is intentionally read-only. There is no create/edit/
 * publish/shortlist/select endpoint here or anywhere in the Admin role;
 * those all live under {@code /api/hr/**} and are guarded separately.
 */
@RestController
@RequestMapping("/api/admin/placements/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPlacementAnalyticsController {

    private final PlacementDashboardService dashboardService;
    private final PlacementReportExportService exportService;

    @GetMapping
    public ApiResponse<AdminPlacementDashboardResponse> getDashboard() {
        return ApiResponse.success(dashboardService.getAdminDashboard());
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf() {
        byte[] pdfBytes = exportService.exportToPdf(dashboardService.getAdminDashboard());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=placement_report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel() {
        byte[] excelBytes = exportService.exportToExcel(dashboardService.getAdminDashboard());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=placement_report.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelBytes);
    }
}
