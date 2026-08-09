package com.infobeans.ibnextstep.studentevaluation;

import com.infobeans.ibnextstep.studentevaluation.dto.StudentEvaluationResponse;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Renders a single StudentEvaluation record as a downloadable PDF or Excel
 * report — for the trainer/admin to save or hand to placement/HR outside
 * the app.
 */
@Service
public class EvaluationExportService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a").withZone(ZoneId.systemDefault());

    public byte[] exportToExcel(StudentEvaluationResponse e) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Evaluation Report");
            int rowIdx = 0;

            rowIdx = writeRow(sheet, rowIdx, "Student", nullToEmpty(e.getStudentName()));
            rowIdx = writeRow(sheet, rowIdx, "Student ID", nullToEmpty(e.getStudentId()));
            rowIdx = writeRow(sheet, rowIdx, "Trainer", nullToEmpty(e.getTrainerName()));
            rowIdx = writeRow(sheet, rowIdx, "Rubric Type", e.getTrainerType() != null ? e.getTrainerType().name() : "");
            rowIdx = writeRow(sheet, rowIdx, "Evaluated At", formatInstant(e.getEvaluatedAt()));
            rowIdx = writeRow(sheet, rowIdx, "Last Updated", e.isEdited() ? formatInstant(e.getUpdatedAt()) + " by " + nullToEmpty(e.getLastEditedBy()) : "Not edited");
            rowIdx++;

            rowIdx = writeRow(sheet, rowIdx, "Attendance %", valueOrNA(e.getAttendancePercentage()));
            rowIdx = writeRow(sheet, rowIdx, "Avg Quiz %", valueOrNA(e.getAvgQuizPercentage()));
            rowIdx = writeRow(sheet, rowIdx, "Avg Coding %", valueOrNA(e.getAvgCodingPercentage()));
            rowIdx = writeRow(sheet, rowIdx, "Avg Mock Interview Rating", valueOrNA(e.getAvgMockInterviewRating()));
            rowIdx = writeRow(sheet, rowIdx, "System Eligible", e.isSystemEligible() ? "Yes" : "No");
            if (e.getSystemIneligibilityReasons() != null && !e.getSystemIneligibilityReasons().isEmpty()) {
                rowIdx = writeRow(sheet, rowIdx, "System Reasons", String.join("; ", e.getSystemIneligibilityReasons()));
            }
            rowIdx++;

            Row skillHeader = sheet.createRow(rowIdx++);
            skillHeader.createCell(0).setCellValue("Rubric Skill");
            skillHeader.createCell(1).setCellValue("Score /10");
            if (e.getSkillScores() != null) {
                for (Map.Entry<String, Integer> entry : e.getSkillScores().entrySet()) {
                    rowIdx = writeRow(sheet, rowIdx, entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
            rowIdx = writeRow(sheet, rowIdx, "Overall Rubric Score", valueOrNA(e.getOverallRubricScore()));
            rowIdx++;

            rowIdx = writeRow(sheet, rowIdx, "Final Eligible", e.isFinalEligible() ? "Yes" : "No");
            if (e.getOverrideReason() != null && !e.getOverrideReason().isBlank()) {
                rowIdx = writeRow(sheet, rowIdx, "Override Reason", e.getOverrideReason());
            }
            rowIdx = writeRow(sheet, rowIdx, "Remarks", nullToEmpty(e.getRemarks()));

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public byte[] exportToPdf(StudentEvaluationResponse e) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Paragraph title = new Paragraph("Student Evaluation Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(16);
            document.add(title);

            Font labelFont = new Font(Font.HELVETICA, 10, Font.BOLD);
            Font valueFont = new Font(Font.HELVETICA, 10);

            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            addKeyValue(header, "Student", nullToEmpty(e.getStudentName()) + " (" + nullToEmpty(e.getStudentId()) + ")", labelFont, valueFont);
            addKeyValue(header, "Trainer", nullToEmpty(e.getTrainerName()) + " — " + (e.getTrainerType() != null ? e.getTrainerType().name() : ""), labelFont, valueFont);
            addKeyValue(header, "Evaluated At", formatInstant(e.getEvaluatedAt()), labelFont, valueFont);
            addKeyValue(header, "Last Updated", e.isEdited() ? formatInstant(e.getUpdatedAt()) + " by " + nullToEmpty(e.getLastEditedBy()) : "Not edited", labelFont, valueFont);
            document.add(header);

            Paragraph metricsTitle = new Paragraph("System Metrics Snapshot", labelFont);
            metricsTitle.setSpacingBefore(14);
            metricsTitle.setSpacingAfter(6);
            document.add(metricsTitle);

            PdfPTable metrics = new PdfPTable(2);
            metrics.setWidthPercentage(100);
            addKeyValue(metrics, "Attendance %", valueOrNA(e.getAttendancePercentage()), labelFont, valueFont);
            addKeyValue(metrics, "Avg Quiz %", valueOrNA(e.getAvgQuizPercentage()), labelFont, valueFont);
            addKeyValue(metrics, "Avg Coding %", valueOrNA(e.getAvgCodingPercentage()), labelFont, valueFont);
            addKeyValue(metrics, "Avg Mock Interview Rating", valueOrNA(e.getAvgMockInterviewRating()), labelFont, valueFont);
            addKeyValue(metrics, "System Eligible", e.isSystemEligible() ? "Yes" : "No", labelFont, valueFont);
            document.add(metrics);

            if (e.getSystemIneligibilityReasons() != null && !e.getSystemIneligibilityReasons().isEmpty()) {
                Paragraph reasons = new Paragraph("System reasons: " + String.join("; ", e.getSystemIneligibilityReasons()), valueFont);
                reasons.setSpacingBefore(6);
                document.add(reasons);
            }

            Paragraph rubricTitle = new Paragraph("Trainer Rubric Scoring", labelFont);
            rubricTitle.setSpacingBefore(14);
            rubricTitle.setSpacingAfter(6);
            document.add(rubricTitle);

            PdfPTable rubricTable = new PdfPTable(2);
            rubricTable.setWidthPercentage(100);
            addCell(rubricTable, "Skill", labelFont);
            addCell(rubricTable, "Score /10", labelFont);
            if (e.getSkillScores() != null) {
                for (Map.Entry<String, Integer> entry : e.getSkillScores().entrySet()) {
                    addCell(rubricTable, entry.getKey(), valueFont);
                    addCell(rubricTable, String.valueOf(entry.getValue()), valueFont);
                }
            }
            document.add(rubricTable);

            Paragraph overall = new Paragraph("Overall Rubric Score: " + valueOrNA(e.getOverallRubricScore()) + " / 10", labelFont);
            overall.setSpacingBefore(10);
            document.add(overall);

            Paragraph verdict = new Paragraph("Final Eligibility: " + (e.isFinalEligible() ? "Eligible" : "Not Eligible"), labelFont);
            verdict.setSpacingBefore(6);
            document.add(verdict);

            if (e.getOverrideReason() != null && !e.getOverrideReason().isBlank()) {
                Paragraph overrideP = new Paragraph("Override Reason: " + e.getOverrideReason(), valueFont);
                overrideP.setSpacingBefore(4);
                document.add(overrideP);
            }

            Paragraph remarksTitle = new Paragraph("Remarks", labelFont);
            remarksTitle.setSpacingBefore(14);
            remarksTitle.setSpacingAfter(4);
            document.add(remarksTitle);
            document.add(new Paragraph(nullToEmpty(e.getRemarks()), valueFont));

            document.close();
            return out.toByteArray();
        } catch (com.lowagie.text.DocumentException | IOException ex) {
            throw new RuntimeException("Failed to generate evaluation PDF report: " + ex.getMessage(), ex);
        }
    }

    private int writeRow(XSSFSheet sheet, int rowIdx, String label, String value) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
        return rowIdx + 1;
    }

    private void addKeyValue(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setPadding(5);
        table.addCell(labelCell);
        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }

    private void addCell(PdfPTable table, String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setPadding(4);
        table.addCell(cell);
    }

    private String formatInstant(java.time.Instant instant) {
        return instant == null ? "" : DATE_FORMAT.format(instant);
    }

    private String valueOrNA(Double value) {
        return value == null ? "N/A" : String.valueOf(value);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
