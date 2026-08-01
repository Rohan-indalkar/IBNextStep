package com.infobeans.ibnextstep.quiz.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** "Short Answer: Evaluate using Gemini AI. Return marks and feedback." */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiShortAnswerGradingService {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GradingResult grade(String questionText, String modelAnswer, String studentAnswer, int maxMarks) {
        String prompt = """
                You are grading a student's short-answer response.

                Question: %s
                Model/expected answer: %s
                Student's answer: %s
                Maximum marks available: %d

                Grade the student's answer for correctness and completeness compared to the model answer.
                Return JSON ONLY, no markdown, no commentary, exactly this shape:
                { "marksAwarded": <number between 0 and %d>, "feedback": "<one or two sentence explanation>" }
                """.formatted(questionText, modelAnswer, studentAnswer, maxMarks, maxMarks);

        try {
            String raw = geminiClient.generate(prompt).trim();
            if (raw.startsWith("```")) {
                raw = raw.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("```\\s*$", "").trim();
            }
            JsonNode node = objectMapper.readTree(raw);
            double marks = node.path("marksAwarded").asDouble(0);
            String feedback = node.path("feedback").asText("");
            return new GradingResult(Math.min(marks, maxMarks), feedback);
        } catch (Exception e) {
            log.error("AI short-answer grading failed, falling back to manual review", e);
            return new GradingResult(-1, "AI grading unavailable — needs manual review");
        }
    }

    public record GradingResult(double marksAwarded, String feedback) {
        public boolean needsManualReview() {
            return marksAwarded < 0;
        }
    }
}
