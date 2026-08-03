package com.infobeans.ibnextstep.codingassessment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infobeans.ibnextstep.codingassessment.dto.CreateQuestionRequest;
import com.infobeans.ibnextstep.codingassessment.dto.GenerateQuestionsRequest;
import com.infobeans.ibnextstep.common.exception.BadRequestException;
import com.infobeans.ibnextstep.quiz.ai.GeminiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Reuses the SAME GeminiClient already built for AI Quiz generation —
 * deliberately not a second Gemini HTTP integration. This service only
 * knows how to build coding-question prompts and parse coding-question
 * JSON; GeminiClient itself is generic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiCodingQuestionService {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<CreateQuestionRequest> generate(GenerateQuestionsRequest request) {
        String prompt = buildPrompt(request);
        String raw = geminiClient.generate(prompt);
        return parse(raw, request);
    }

    private String buildPrompt(GenerateQuestionsRequest request) {
        String difficulty = request.getDifficulty() == null ? "Mixed (vary across Easy/Medium/Hard)" : request.getDifficulty().name();

        return """
                Generate %d coding interview questions about "%s" in %s.
                Difficulty: %s

                Return JSON ONLY — a JSON array, no markdown fences, no commentary — where each element matches exactly:
                {
                  "title": "...",
                  "problemStatement": "...",
                  "inputFormat": "...",
                  "outputFormat": "...",
                  "constraints": "...",
                  "examples": [ { "input": "...", "output": "...", "explanation": "..." } ],
                  "difficulty": "EASY | MEDIUM | HARD",
                  "marks": 10,
                  "timeLimitSeconds": 2,
                  "memoryLimitMb": 256,
                  "publicTestCases": [ { "input": "...", "expectedOutput": "..." } ],
                  "hiddenTestCases": [ { "input": "...", "expectedOutput": "..." } ]
                }
                Rules:
                - Provide at least 2 public test cases and at least 3 hidden test cases per question.
                - Input/output values must be exact strings a program would read from stdin and write to stdout.
                - Generate exactly %d questions.
                """.formatted(request.getQuestionCount(), request.getTopic(), request.getLanguage(), difficulty, request.getQuestionCount());
    }

    private List<CreateQuestionRequest> parse(String raw, GenerateQuestionsRequest request) {
        String cleaned = raw.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("```\\s*$", "").trim();
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(cleaned);
        } catch (Exception e) {
            log.error("Failed to parse AI coding-question response: {}", raw);
            throw new BadRequestException("AI response was not valid JSON — try regenerating");
        }

        if (!root.isArray()) {
            throw new BadRequestException("AI did not return a JSON array of questions as expected");
        }

        List<CreateQuestionRequest> questions = new ArrayList<>();
        for (JsonNode node : root) {
            try {
                CreateQuestionRequest q = objectMapper.treeToValue(node, CreateQuestionRequest.class);
                // allowedLanguages is an input the trainer already chose — AI doesn't invent it.
                q.setAllowedLanguages(List.of(request.getLanguage()));
                questions.add(q);
            } catch (Exception e) {
                log.warn("Skipped one malformed AI question: {}", node);
            }
        }

        if (questions.isEmpty()) {
            throw new BadRequestException("AI returned zero usable questions — try adjusting the topic");
        }
        return questions;
    }
}
