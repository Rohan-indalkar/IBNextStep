package com.infobeans.ibnextstep.quiz.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infobeans.ibnextstep.common.exception.BadRequestException;
import com.infobeans.ibnextstep.quiz.Difficulty;
import com.infobeans.ibnextstep.quiz.Question;
import com.infobeans.ibnextstep.quiz.QuestionType;
import com.infobeans.ibnextstep.quiz.dto.GenerateQuizRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the prompt, calls Gemini, and turns its JSON text response into
 * real Question objects. This is the ONLY place quiz-generation prompts are
 * constructed — Controller and QuizService never see raw prompt text.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiQuizGenerationService {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Question> generateQuestions(GenerateQuizRequest request) {
        String prompt = buildPrompt(request);
        String rawResponse = geminiClient.generate(prompt);
        return parseQuestions(rawResponse, request);
    }

    /** Used by "Regenerate Single Question" — same JSON contract, just one question instead of a list. */
    public Question regenerateSingle(String topic, Difficulty difficulty, QuestionType type, String additionalInstruction) {
        String prompt = """
                Generate exactly ONE %s-difficulty %s question about "%s".
                %s

                Return JSON ONLY, no markdown, no explanation outside the JSON, matching exactly this shape:
                {
                  "questionText": "...",
                  "options": ["...", "...", "...", "..."],
                  "correctAnswer": "...",
                  "correctAnswers": [],
                  "explanation": "...",
                  "marks": 1
                }
                For TRUE_FALSE questions, options must be exactly ["True", "False"].
                For MULTIPLE_SELECT questions, put every correct option in "correctAnswers" and leave "correctAnswer" empty.
                For FILL_BLANK and SHORT_ANSWER questions, options must be an empty array.
                """.formatted(difficulty, type, topic, additionalInstruction == null ? "" : "Additional instruction: " + additionalInstruction);

        String rawResponse = geminiClient.generate(prompt);
        JsonNode node = parseJson(rawResponse);
        return toQuestion(node, type, difficulty);
    }

    private String buildPrompt(GenerateQuizRequest request) {
        String subTopics = request.getSubTopics() == null || request.getSubTopics().isEmpty()
                ? "" : "Sub-topics to cover: " + String.join(", ", request.getSubTopics());

        String types = request.getQuestionTypes().stream().map(Enum::name).reduce((a, b) -> a + ", " + b).orElse("MCQ");

        return """
                %s

                Topic: %s
                %s
                Difficulty: %s
                Number of questions: %d
                Duration: %d minutes
                Passing percentage: %.0f%%
                Language: %s
                Allowed question types: %s

                Return JSON ONLY — no markdown code fences, no commentary before or after — as a JSON array where each element matches exactly this shape:
                {
                  "questionText": "...",
                  "options": ["...", "...", "...", "..."],
                  "correctAnswer": "...",
                  "correctAnswers": [],
                  "explanation": "...",
                  "type": "MCQ | TRUE_FALSE | MULTIPLE_SELECT | FILL_BLANK | SHORT_ANSWER",
                  "difficulty": "EASY | MEDIUM | HARD",
                  "marks": 1
                }
                Rules:
                - TRUE_FALSE questions: options must be exactly ["True", "False"].
                - MULTIPLE_SELECT questions: put every correct option in "correctAnswers", leave "correctAnswer" as an empty string.
                - FILL_BLANK and SHORT_ANSWER questions: options must be an empty array.
                - Generate exactly %d questions total.
                """.formatted(request.getPrompt(), request.getTopic(), subTopics, request.getDifficulty(),
                request.getQuestionCount(), request.getDurationMinutes(), request.getPassingPercentage(),
                request.getLanguage(), types, request.getQuestionCount());
    }

    private List<Question> parseQuestions(String rawResponse, GenerateQuizRequest request) {
        JsonNode root = parseJson(rawResponse);
        if (!root.isArray()) {
            throw new BadRequestException("AI did not return a JSON array of questions as expected");
        }

        List<Question> questions = new ArrayList<>();
        for (JsonNode node : root) {
            QuestionType type = node.hasNonNull("type")
                    ? QuestionType.valueOf(node.get("type").asText())
                    : request.getQuestionTypes().get(0);
            Difficulty difficulty = node.hasNonNull("difficulty")
                    ? Difficulty.valueOf(node.get("difficulty").asText())
                    : request.getDifficulty();
            questions.add(toQuestion(node, type, difficulty));
        }

        if (questions.isEmpty()) {
            throw new BadRequestException("AI returned zero questions — try adjusting the prompt");
        }
        return questions;
    }

    private Question toQuestion(JsonNode node, QuestionType type, Difficulty difficulty) {
        List<String> options = new ArrayList<>();
        if (node.has("options") && node.get("options").isArray()) {
            node.get("options").forEach(o -> options.add(o.asText()));
        }

        List<String> correctAnswers = new ArrayList<>();
        if (node.has("correctAnswers") && node.get("correctAnswers").isArray()) {
            node.get("correctAnswers").forEach(o -> correctAnswers.add(o.asText()));
        }

        return Question.builder()
                .questionText(node.path("questionText").asText())
                .options(options)
                .correctAnswer(node.path("correctAnswer").asText(null))
                .correctAnswers(correctAnswers)
                .explanation(node.path("explanation").asText(null))
                .type(type)
                .difficulty(difficulty)
                .marks(node.path("marks").asInt(1))
                .build();
    }

    /** Gemini frequently wraps JSON in ```json ... ``` fences even when told not to — strip them before parsing. */
    private JsonNode parseJson(String rawResponse) {
        String cleaned = rawResponse.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("```\\s*$", "").trim();
        }
        try {
            return objectMapper.readTree(cleaned);
        } catch (Exception e) {
            log.error("Failed to parse AI JSON response: {}", rawResponse);
            throw new BadRequestException("AI response was not valid JSON — try regenerating");
        }
    }
}
