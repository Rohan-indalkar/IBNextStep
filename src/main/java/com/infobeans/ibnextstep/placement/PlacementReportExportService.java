package com.infobeans.ibnextstep.placement;

import com.infobeans.ibnextstep.placement.dto.AdminPlacementDashboardResponse;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

/**
 * Renders the admin's read-only placement analytics dashboard as a
 * downloadable PDF or Excel report, mirroring the same openpdf/POI
 * approach {@code EvaluationExportService} uses elsewhere in the app.
 */
@Service
public class PlacementReportExportService {

    public byte[] exportToExcel(AdminPlacementDashboardResponse d) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Placement Report");
            int rowIdx = 0;

            rowIdx = writeRow(sheet, rowIdx, "Total Students", String.valueOf(d.getTotalStudents()));
            rowIdx = writeRow(sheet, rowIdx, "Active Students", String.valueOf(d.getActiveStudents()));
            rowIdx = writeRow(sheet, rowIdx, "Placed Students", String.valueOf(d.getPlacedStudents()));
            rowIdx = writeRow(sheet, rowIdx, "Unplaced Students", String.valueOf(d.getUnplacedStudents()));
            rowIdx = writeRow(sheet, rowIdx, "Placement %", d.getPlacementPercentage() + "%");
            rowIdx++;

            rowIdx = writeRow(sheet, rowIdx, "Total Companies", String.valueOf(d.getTotalCompanies()));
            rowIdx = writeRow(sheet, rowIdx, "Active Companies", String.valueOf(d.getActiveCompanies()));
            rowIdx = writeRow(sheet, rowIdx, "Campus Drives", String.valueOf(d.getCampusDrives()));
            rowIdx = writeRow(sheet, rowIdx, "Off Campus Drives", String.valueOf(d.getOffCampusDrives()));
            rowIdx++;

            rowIdx = writeRow(sheet, rowIdx, "Applications", String.valueOf(d.getApplications()));
            rowIdx = writeRow(sheet, rowIdx, "Selections", String.valueOf(d.getSelections()));
            rowIdx = writeRow(sheet, rowIdx, "Rejections", String.valueOf(d.getRejections()));
            rowIdx = writeRow(sheet, rowIdx, "Highest Package (LPA)", valueOrNA(d.getHighestPackageLpa()));
            rowIdx = writeRow(sheet, rowIdx, "Average Package (LPA)", valueOrNA(d.getAveragePackageLpa()));
            rowIdx++;

            rowIdx = writeSection(sheet, rowIdx, "Department-wise Placement", d.getDepartmentWisePlacement());
            rowIdx = writeSection(sheet, rowIdx, "Company-wise Placement", d.getCompanyWisePlacement());
            rowIdx = writeSection(sheet, rowIdx, "Monthly Placement Trend", d.getMonthlyPlacementTrend());
            writeSection(sheet, rowIdx, "Year-wise Placement", d.getYearWisePlacement());

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public byte[] exportToPdf(AdminPlacementDashboardResponse d) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL);

            Paragraph title = new Paragraph("Placement Analytics Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            addKeyValueTable(document, headerFont, normalFont, "Student Overview", Map.of(
                    "Total Students", String.valueOf(d.getTotalStudents()),
                    "Active Students", String.valueOf(d.getActiveStudents()),
                    "Placed Students", String.valueOf(d.getPlacedStudents()),
                    "Unplaced Students", String.valueOf(d.getUnplacedStudents()),
                    "Placement %", d.getPlacementPercentage() + "%"
            ));

            addKeyValueTable(document, headerFont, normalFont, "Drives", Map.of(
                    "Total Companies", String.valueOf(d.getTotalCompanies()),
                    "Active Companies", String.valueOf(d.getActiveCompanies()),
                    "Campus Drives", String.valueOf(d.getCampusDrives()),
                    "Off Campus Drives", String.valueOf(d.getOffCampusDrives())
            ));

            addKeyValueTable(document, headerFont, normalFont, "Applications", Map.of(
                    "Applications", String.valueOf(d.getApplications()),
                    "Selections", String.valueOf(d.getSelections()),
                    "Rejections", String.valueOf(d.getRejections()),
                    "Highest Package (LPA)", valueOrNA(d.getHighestPackageLpa()),
                    "Average Package (LPA)", valueOrNA(d.getAveragePackageLpa())
            ));

            addLongMapTable(document, headerFont, normalFont, "Department-wise Placement", d.getDepartmentWisePlacement());
            addLongMapTable(document, headerFont, normalFont, "Company-wise Placement", d.getCompanyWisePlacement());
            addLongMapTable(document, headerFont, normalFont, "Monthly Placement Trend", d.getMonthlyPlacementTrend());
            addLongMapTable(document, headerFont, normalFont, "Year-wise Placement", d.getYearWisePlacement());

            document.close();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private void addKeyValueTable(Document document, Font headerFont, Font normalFont, String title, Map<String, String> rows) throws IOException {
        try {
            document.add(new Paragraph(title, headerFont));
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            for (Map.Entry<String, String> entry : rows.entrySet()) {
                table.addCell(cell(entry.getKey(), normalFont));
                table.addCell(cell(entry.getValue(), normalFont));
            }
            document.add(table);
            document.add(new Paragraph(" "));
        } catch (com.lowagie.text.DocumentException e) {
            throw new IOException(e);
        }
    }

    private void addLongMapTable(Document document, Font headerFont, Font normalFont, String title, Map<String, Long> rows) throws IOException {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        try {
            document.add(new Paragraph(title, headerFont));
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            for (Map.Entry<String, Long> entry : rows.entrySet()) {
                table.addCell(cell(entry.getKey(), normalFont));
                table.addCell(cell(String.valueOf(entry.getValue()), normalFont));
            }
            document.add(table);
            document.add(new Paragraph(" "));
        } catch (com.lowagie.text.DocumentException e) {
            throw new IOException(e);
        }
    }

    private PdfPCell cell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(4);
        return cell;
    }

    private int writeRow(XSSFSheet sheet, int rowIdx, String label, String value) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
        return rowIdx + 1;
    }

    private int writeSection(XSSFSheet sheet, int rowIdx, String title, Map<String, Long> rows) {
        if (rows == null || rows.isEmpty()) {
            return rowIdx;
        }
        Row header = sheet.createRow(rowIdx++);
        header.createCell(0).setCellValue(title);
        for (Map.Entry<String, Long> entry : rows.entrySet()) {
            rowIdx = writeRow(sheet, rowIdx, entry.getKey(), String.valueOf(entry.getValue()));
        }
        return rowIdx + 1;
    }

    private String valueOrNA(Double value) {
        return value == null ? "N/A" : String.valueOf(value);
    }
}
