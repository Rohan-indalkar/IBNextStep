package com.infobeans.ibnextstep.resume.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infobeans.ibnextstep.common.exception.BadRequestException;
import com.infobeans.ibnextstep.quiz.ai.GeminiClient;
import com.infobeans.ibnextstep.resume.ResumeAiAnalysis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * "Analyze with AI" on the trainer resume-review screen — reuses the same
 * GeminiClient the Quiz and Assignment modules talk to. Nothing above this
 * layer (Controller, ResumeService) ever builds a prompt or touches Gemini
 * directly, matching the Controller -> Service -> AI Service -> Gemini
 * layering used elsewhere in the codebase.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeAiService {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    public ResumeAiAnalysis analyze(String resumeText) {
        String prompt = buildPrompt(resumeText);
        String raw = geminiClient.generate(prompt);
        return parse(raw);
    }

    private String buildPrompt(String resumeText) {
        return """
                You are an experienced technical recruiter and resume coach reviewing a
                fresher/entry-level candidate's resume for a corporate training and
                placement program. Be constructive, specific, and honest — this feedback
                goes to a trainer who will pass it on to the student.

                Resume text (extracted from a PDF — formatting/line breaks may be imperfect):
                ---
                %s
                ---

                Evaluate: clarity and structure, use of action verbs and quantifiable
                impact, technical skills presentation, projects, education, formatting /
                ATS-friendliness, and any spelling or grammar issues you notice.

                Return ONLY a JSON object (no markdown fences, no commentary before or
                after) matching exactly this shape:
                {
                  "overallScore": 0-100,
                  "summary": "2-3 sentence overall impression",
                  "strengths": ["short point", "..."],
                  "weaknesses": ["short point", "..."],
                  "missingSections": ["e.g. Projects, Certifications, Professional Summary"],
                  "atsIssues": ["formatting or ATS-parsing concerns, if any"],
                  "suggestions": ["specific, actionable improvement", "..."],
                  "suggestedReviewText": "A ready-to-edit paragraph a trainer could send the student as written feedback, 3-5 sentences"
                }
                Keep each array item concise (under 20 words). If a category has nothing
                notable, return an empty array for it rather than omitting the key.
                """.formatted(resumeText);
    }

    private ResumeAiAnalysis parse(String raw) {
        try {
            JsonNode node = objectMapper.readTree(stripCodeFences(raw));

            return ResumeAiAnalysis.builder()
                    .overallScore(node.path("overallScore").isMissingNode() || node.path("overallScore").isNull()
                            ? null : node.path("overallScore").asInt())
                    .summary(textOrNull(node, "summary"))
                    .strengths(toList(node.path("strengths")))
                    .weaknesses(toList(node.path("weaknesses")))
                    .missingSections(toList(node.path("missingSections")))
                    .atsIssues(toList(node.path("atsIssues")))
                    .suggestions(toList(node.path("suggestions")))
                    .suggestedReviewText(textOrNull(node, "suggestedReviewText"))
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse Gemini resume analysis response: {}", raw, e);
            throw new BadRequestException("Failed to parse AI resume analysis: " + e.getMessage());
        }
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private List<String> toList(JsonNode arrayNode) {
        List<String> list = new ArrayList<>();
        if (arrayNode.isArray()) {
            for (JsonNode item : arrayNode) {
                String text = item.asText(null);
                if (text != null && !text.isBlank()) {
                    list.add(text.trim());
                }
            }
        }
        return list;
    }

    private String stripCodeFences(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\s*", "");
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }
}
