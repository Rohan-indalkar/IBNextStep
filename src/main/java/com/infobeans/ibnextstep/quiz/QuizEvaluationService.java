package com.infobeans.ibnextstep.quiz;

import com.infobeans.ibnextstep.quiz.ai.AiShortAnswerGradingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Auto-evaluation rules straight from spec:
 * MCQ / TRUE_FALSE -> compare value. MULTIPLE_SELECT -> compare option sets.
 * FILL_BLANK -> case-insensitive, trimmed compare. SHORT_ANSWER -> Gemini grades it.
 */
@Service
@RequiredArgsConstructor
public class QuizEvaluationService {

    private final AiShortAnswerGradingService aiGradingService;

    public record QuestionResult(boolean correct, double marksAwarded, boolean pendingManualReview) {}

    public QuestionResult evaluate(QuizAttempt.AssignedQuestion assigned, QuestionBankItem bankItem, List<String> studentAnswer) {
        Question question = bankItem.getQuestion();

        if (studentAnswer == null || studentAnswer.isEmpty()) {
            return new QuestionResult(false, 0, false);
        }

        return switch (assigned.getType()) {
            case MCQ, TRUE_FALSE -> {
                boolean correct = question.getCorrectAnswer() != null
                        && question.getCorrectAnswer().trim().equalsIgnoreCase(studentAnswer.get(0).trim());
                yield new QuestionResult(correct, correct ? assigned.getMarks() : 0, false);
            }
            case MULTIPLE_SELECT -> {
                List<String> correctSet = question.getCorrectAnswers() == null ? List.of() : question.getCorrectAnswers();
                boolean sameSize = correctSet.size() == studentAnswer.size();
                boolean sameContent = sameSize && correctSet.stream()
                        .allMatch(c -> studentAnswer.stream().anyMatch(a -> a.trim().equalsIgnoreCase(c.trim())));
                yield new QuestionResult(sameContent, sameContent ? assigned.getMarks() : 0, false);
            }
            case FILL_BLANK -> {
                boolean correct = question.getCorrectAnswer() != null
                        && question.getCorrectAnswer().trim().equalsIgnoreCase(studentAnswer.get(0).trim());
                yield new QuestionResult(correct, correct ? assigned.getMarks() : 0, false);
            }
            case SHORT_ANSWER -> {
                var grading = aiGradingService.grade(assigned.getQuestionText(),
                        Optional.ofNullable(question.getCorrectAnswer()).orElse(question.getExplanation()),
                        studentAnswer.get(0), assigned.getMarks());
                if (grading.needsManualReview()) {
                    yield new QuestionResult(false, 0, true);
                }
                yield new QuestionResult(grading.marksAwarded() >= assigned.getMarks() * 0.5, grading.marksAwarded(), false);
            }
        };
    }
}
