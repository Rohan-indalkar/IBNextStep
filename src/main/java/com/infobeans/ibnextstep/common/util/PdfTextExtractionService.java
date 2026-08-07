package com.infobeans.ibnextstep.common.util;

import com.infobeans.ibnextstep.common.exception.BadRequestException;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * Pulls raw text out of a PDF using openpdf (already on the classpath for
 * other PDF handling). Used by the resume AI analyzer today; written as a
 * standalone service so any other module needing "read text out of a PDF"
 * (study materials, placement brochures, etc.) can reuse it later instead
 * of re-implementing extraction.
 */
@Service
@Slf4j
public class PdfTextExtractionService {

    // Resumes are 1-3 pages; capping protects against someone uploading a huge
    // multi-page PDF and blowing up the Gemini prompt size / cost.
    private static final int MAX_PAGES = 10;
    private static final int MAX_CHARS = 15_000;

    /** @param fileName only used to validate the extension — callers pass the stream separately. */
    public String extractText(InputStream inputStream, String fileName) {
        if (fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
            throw new BadRequestException("AI resume analysis currently supports PDF files only");
        }

        try {
            PdfReader reader = new PdfReader(inputStream);
            try {
                PdfTextExtractor extractor = new PdfTextExtractor(reader);
                StringBuilder text = new StringBuilder();
                int pagesToRead = Math.min(reader.getNumberOfPages(), MAX_PAGES);

                for (int page = 1; page <= pagesToRead; page++) {
                    text.append(extractor.getTextFromPage(page)).append("\n");
                    if (text.length() > MAX_CHARS) {
                        break;
                    }
                }

                String result = text.toString().trim();
                if (result.isBlank()) {
                    throw new BadRequestException(
                            "Could not extract any text from this resume — it may be a scanned image rather than text-based PDF");
                }

                return result.length() > MAX_CHARS ? result.substring(0, MAX_CHARS) : result;
            } finally {
                reader.close();
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to extract text from PDF resume {}", fileName, e);
            throw new BadRequestException("Failed to read the resume PDF: " + e.getMessage());
        }
    }
}
