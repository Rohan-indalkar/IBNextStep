package com.infobeans.ibnextstep.dashboard;

import com.infobeans.ibnextstep.user.User;
import com.infobeans.ibnextstep.user.UserRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportExportService {

    private final UserRepository userRepository;

    public byte[] exportUsersToExcel() {
        List<User> users = userRepository.findAll();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Users");

            String[] headers = {"First Name", "Last Name", "Email", "Role", "Trainer Type", "Status", "Created At"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            int rowIdx = 1;
            for (User user : users) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(user.getFirstName());
                row.createCell(1).setCellValue(user.getLastName());
                row.createCell(2).setCellValue(user.getEmail());
                row.createCell(3).setCellValue(user.getRole().name());
                row.createCell(4).setCellValue(user.getTrainerType() != null ? user.getTrainerType().name() : "");
                row.createCell(5).setCellValue(user.getStatus().name());
                Cell dateCell = row.createCell(6);
                dateCell.setCellValue(user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] exportUsersToPdf() {
        List<User> users = userRepository.findAll();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Paragraph title = new Paragraph("User Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(16);
            document.add(title);

            String[] headers = {"First Name", "Last Name", "Email", "Role", "Trainer Type", "Status", "Created At"};
            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);

            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD);
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setPadding(5);
                table.addCell(cell);
            }

            Font cellFont = new Font(Font.HELVETICA, 9);
            for (User user : users) {
                addCell(table, nullToEmpty(user.getFirstName()), cellFont);
                addCell(table, nullToEmpty(user.getLastName()), cellFont);
                addCell(table, nullToEmpty(user.getEmail()), cellFont);
                addCell(table, user.getRole() != null ? user.getRole().name() : "", cellFont);
                addCell(table, user.getTrainerType() != null ? user.getTrainerType().name() : "", cellFont);
                addCell(table, user.getStatus() != null ? user.getStatus().name() : "", cellFont);
                addCell(table, user.getCreatedAt() != null ? user.getCreatedAt().toString() : "", cellFont);
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (com.lowagie.text.DocumentException | IOException e) {
            throw new RuntimeException("Failed to generate PDF report: " + e.getMessage(), e);
        }
    }

    public java.util.List<ReportInfo> availableReports(String query) {
        java.util.List<ReportInfo> catalog = java.util.List.of(
                new ReportInfo("system-summary", "System Summary", "Counts of students, trainers, HR, active batches, courses, and pending approvals", "Overview"),
                new ReportInfo("users-export", "User Directory Export", "Full list of all users with role, status, and trainer type", "Users"),
                new ReportInfo("batch-status", "Batch Status Report", "Active vs inactive batches with enrollment counts", "Batches"),
                new ReportInfo("placement-approvals", "Placement Approval Log", "Approved and rejected placement opportunities", "Placements"),
                new ReportInfo("audit-activity", "Audit Activity Report", "Recent login, logout, and record-change events", "Audit")
        );

        if (query == null || query.isBlank()) {
            return catalog;
        }

        String q = query.toLowerCase();
        return catalog.stream()
                .filter(r -> r.getName().toLowerCase().contains(q)
                        || r.getDescription().toLowerCase().contains(q)
                        || r.getCategory().toLowerCase().contains(q))
                .toList();
    }

    private void addCell(PdfPTable table, String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setPadding(4);
        table.addCell(cell);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
