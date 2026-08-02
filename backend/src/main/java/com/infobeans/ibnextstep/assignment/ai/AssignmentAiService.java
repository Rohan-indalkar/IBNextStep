package com.infobeans.ibnextstep.assignment.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infobeans.ibnextstep.assignment.AssignmentQuestion;
import com.infobeans.ibnextstep.common.exception.BadRequestException;
import com.infobeans.ibnextstep.material.DifficultyLevel;
import com.infobeans.ibnextstep.quiz.ai.GeminiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * "Create assignment through AI" — generates open-ended interview-style
 * prompts (no options, no correct answer, unlike the quiz generator).
 * Reuses the same GeminiClient the Quiz module talks to.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssignmentAiService {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    public List<AssignmentQuestion> generateQuestions(String topic, String module, DifficultyLevel difficultyLevel,
                                                        int numberOfQuestions, String additionalInstructions) {
        String prompt = """
                You are generating open-ended interview practice questions for a corporate
                training platform. These are NOT multiple-choice — students will answer with
                free text and/or an uploaded file.

                Topic: %s
                Module: %s
                Difficulty level: %s
                Number of questions: %d
                %s

                Return ONLY a JSON array of strings (no markdown fences, no commentary), where
                each string is one complete interview question, e.g.
                ["Explain the difference between == and .equals() in Java.", "..."]
                """.formatted(
                topic, module == null || module.isBlank() ? "General" : module,
                difficultyLevel, numberOfQuestions,
                additionalInstructions == null || additionalInstructions.isBlank()
                        ? "" : "Additional instructions: " + additionalInstructions);

        String raw = geminiClient.generate(prompt);
        return parseQuestions(raw);
    }

    private List<AssignmentQuestion> parseQuestions(String raw) {
        try {
            JsonNode array = objectMapper.readTree(stripCodeFences(raw));
            if (!array.isArray()) {
                throw new BadRequestException("AI response was not a JSON array of questions");
            }
            List<AssignmentQuestion> questions = new ArrayList<>();
            int order = 0;
            for (JsonNode node : array) {
                String text = node.asText(null);
                if (text == null || text.isBlank()) continue;
                questions.add(AssignmentQuestion.builder()
                        .id(UUID.randomUUID().toString())
                        .questionText(text.trim())
                        .orderIndex(order++)
                        .build());
            }
            if (questions.isEmpty()) {
                throw new BadRequestException("AI did not return any usable questions — try again or adjust the prompt");
            }
            return questions;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse Gemini assignment question response: {}", raw, e);
            throw new BadRequestException("Failed to parse AI-generated questions: " + e.getMessage());
        }
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
