package com.infobeans.ibnextstep.attendance;

import com.infobeans.ibnextstep.attendance.dto.AttendanceRecordResponse;
import com.infobeans.ibnextstep.attendance.dto.StudentMonthlyAttendanceResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/** "Download Attendance Report (Optional)" — the student's own monthly report as an .xlsx file. */
@Service
@RequiredArgsConstructor
public class AttendanceReportService {

    private final AttendanceService attendanceService;

    public byte[] exportMyMonthlyReport(String studentEmail, int year, int month) {
        StudentMonthlyAttendanceResponse summary = attendanceService.getMyMonthlyAttendance(studentEmail, year, month);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Attendance");

            Row title = sheet.createRow(0);
            title.createCell(0).setCellValue("Attendance report — " + month + "/" + year);

            Row summaryRow = sheet.createRow(1);
            summaryRow.createCell(0).setCellValue("Present: " + summary.getPresentCount());
            summaryRow.createCell(1).setCellValue("Absent: " + summary.getAbsentCount());
            summaryRow.createCell(2).setCellValue("Late: " + summary.getLateCount());
            summaryRow.createCell(3).setCellValue("Percentage: " + summary.getAttendancePercentage() + "%");

            Row headerRow = sheet.createRow(3);
            headerRow.createCell(0).setCellValue("Date");
            headerRow.createCell(1).setCellValue("Status");

            int rowIdx = 4;
            for (AttendanceRecordResponse record : summary.getRecords()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(record.getDate().toString());
                row.createCell(1).setCellValue(record.getStatus().name());
            }

            for (int i = 0; i < 4; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
