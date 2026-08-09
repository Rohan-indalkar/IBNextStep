package com.infobeans.ibnextstep.quiz;

import com.infobeans.ibnextstep.audit.AuditLogService;
import com.infobeans.ibnextstep.batch.Batch;
import com.infobeans.ibnextstep.batch.BatchRepository;
import com.infobeans.ibnextstep.common.PagedResponse;
import com.infobeans.ibnextstep.common.exception.BadRequestException;
import com.infobeans.ibnextstep.common.exception.ResourceNotFoundException;
import com.infobeans.ibnextstep.notification.NotificationService;
import com.infobeans.ibnextstep.quiz.ai.AiQuizGenerationService;
import com.infobeans.ibnextstep.quiz.dto.*;
import com.infobeans.ibnextstep.user.Role;
import com.infobeans.ibnextstep.user.User;
import com.infobeans.ibnextstep.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class TrainerQuizService {

    private final QuizRepository quizRepository;
    private final QuestionBankRepository questionBankRepository;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;
    private final AiQuizGenerationService aiQuizGenerationService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    /** Controller -> (this) Quiz Service -> AI Quiz Service -> Gemini API. Trainer never writes questions themselves. */
    public Quiz generate(String trainerEmail, GenerateQuizRequest request) {
        User trainer = getTrainer(trainerEmail);
        verifyBatchOwnership(trainer.getId(), request.getBatchId());

        List<Question> generated = aiQuizGenerationService.generateQuestions(request);

        List<Quiz.QuizQuestionEntry> entries = new ArrayList<>();
        int order = 0;
        for (Question q : generated) {
            // Every AI-generated question is permanently stored in the Question Bank for reuse.
            QuestionBankItem bankItem = QuestionBankItem.builder()
                    .question(q)
                    .topic(request.getTopic())
                    .subTopics(request.getSubTopics())
                    .technology(request.getTopic())
                    .generatedByTrainerId(trainer.getId())
                    .aiGenerated(true)
                    .createdAt(Instant.now())
                    .build();
            bankItem = questionBankRepository.save(bankItem);

            entries.add(Quiz.QuizQuestionEntry.builder()
                    .questionBankId(bankItem.getId())
                    .question(q)
                    .order(order++)
                    .build());
        }

        Quiz quiz = Quiz.builder()
                .title(request.getTitle() != null && !request.getTitle().isBlank() ? request.getTitle() : request.getTopic() + " Quiz")
                .prompt(request.getPrompt())
                .topic(request.getTopic())
                .subTopics(request.getSubTopics())
                .difficulty(request.getDifficulty())
                .questionCount(entries.size())
                .durationMinutes(request.getDurationMinutes())
                .passingPercentage(request.getPassingPercentage())
                .batchId(request.getBatchId())
                .language(request.getLanguage())
                .questionTypes(request.getQuestionTypes())
                .questions(entries)
                .status(QuizStatus.DRAFT)
                .trainerId(trainer.getId())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        quiz = quizRepository.save(quiz);
        audit(trainer, "QUIZ_GENERATED", "AI-generated quiz '" + quiz.getTitle() + "' (" + entries.size() + " questions)");
        return quiz;
    }

    public Quiz update(String trainerEmail, String quizId, UpdateQuizRequest request) {
        Quiz quiz = getOwnedDraftQuiz(trainerEmail, quizId);
        if (request.getTitle() != null) quiz.setTitle(request.getTitle());
        if (request.getDurationMinutes() != null) quiz.setDurationMinutes(request.getDurationMinutes());
        if (request.getPassingPercentage() != null) quiz.setPassingPercentage(request.getPassingPercentage());
        quiz.setUpdatedAt(Instant.now());
        return quizRepository.save(quiz);
    }

    public Quiz editQuestion(String trainerEmail, String quizId, int questionOrder, EditQuestionRequest request) {
        Quiz quiz = getOwnedDraftQuiz(trainerEmail, quizId);
        Quiz.QuizQuestionEntry entry = findEntry(quiz, questionOrder);
        entry.setQuestion(request.getQuestion());
        quiz.setUpdatedAt(Instant.now());
        return quizRepository.save(quiz);
    }

    public Quiz deleteQuestion(String trainerEmail, String quizId, int questionOrder) {
        Quiz quiz = getOwnedDraftQuiz(trainerEmail, quizId);
        boolean removed = quiz.getQuestions().removeIf(e -> e.getOrder() == questionOrder);
        if (!removed) {
            throw new ResourceNotFoundException("No question at position " + questionOrder);
        }
        quiz.setQuestionCount(quiz.getQuestions().size());
        quiz.setUpdatedAt(Instant.now());
        return quizRepository.save(quiz);
    }

    public Quiz addQuestion(String trainerEmail, String quizId, AddQuestionRequest request) {
        Quiz quiz = getOwnedDraftQuiz(trainerEmail, quizId);
        int nextOrder = quiz.getQuestions().stream().mapToInt(Quiz.QuizQuestionEntry::getOrder).max().orElse(-1) + 1;
        quiz.getQuestions().add(Quiz.QuizQuestionEntry.builder()
                .question(request.getQuestion())
                .order(nextOrder)
                .build());
        quiz.setQuestionCount(quiz.getQuestions().size());
        quiz.setUpdatedAt(Instant.now());
        return quizRepository.save(quiz);
    }

    public Quiz regenerateQuestion(String trainerEmail, String quizId, int questionOrder, RegenerateQuestionRequest request) {
        Quiz quiz = getOwnedDraftQuiz(trainerEmail, quizId);
        Quiz.QuizQuestionEntry entry = findEntry(quiz, questionOrder);

        Question replacement = aiQuizGenerationService.regenerateSingle(
                quiz.getTopic(), entry.getQuestion().getDifficulty(), entry.getQuestion().getType(),
                request.getAdditionalInstruction());

        entry.setQuestion(replacement);
        quiz.setUpdatedAt(Instant.now());
        return quizRepository.save(quiz);
    }

    public Quiz regenerateEntireQuiz(String trainerEmail, String quizId) {
        Quiz existing = getOwnedDraftQuiz(trainerEmail, quizId);

        GenerateQuizRequest request = new GenerateQuizRequest();
        request.setTitle(existing.getTitle());
        request.setPrompt(existing.getPrompt());
        request.setTopic(existing.getTopic());
        request.setSubTopics(existing.getSubTopics());
        request.setDifficulty(existing.getDifficulty());
        request.setQuestionCount(existing.getQuestionCount());
        request.setDurationMinutes(existing.getDurationMinutes());
        request.setPassingPercentage(existing.getPassingPercentage());
        request.setBatchId(existing.getBatchId());
        request.setLanguage(existing.getLanguage());
        request.setQuestionTypes(existing.getQuestionTypes());

        List<Question> generated = aiQuizGenerationService.generateQuestions(request);
        List<Quiz.QuizQuestionEntry> entries = IntStream.range(0, generated.size())
                .mapToObj(i -> Quiz.QuizQuestionEntry.builder().question(generated.get(i)).order(i).build())
                .toList();

        existing.setQuestions(new ArrayList<>(entries));
        existing.setUpdatedAt(Instant.now());
        return quizRepository.save(existing);
    }

    public void delete(String trainerEmail, String quizId) {
        Quiz quiz = getOwnedDraftQuiz(trainerEmail, quizId);
        quizRepository.delete(quiz);
    }

    public Quiz publish(String trainerEmail, String quizId) {
        Quiz quiz = getOwnedDraftQuiz(trainerEmail, quizId);
        if (quiz.getQuestions().isEmpty()) {
            throw new BadRequestException("Cannot publish a quiz with no questions");
        }

        Batch batch = batchRepository.findById(quiz.getBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + quiz.getBatchId()));

        quiz.setStatus(QuizStatus.PUBLISHED);
        quiz.setPublishedAt(Instant.now());
        quiz.setUpdatedAt(Instant.now());
        Quiz publishedQuiz = quizRepository.save(quiz);

        for (String studentId : batch.getStudentIds()) {
            notificationService.sendToUser(studentId, "New Quiz Assigned: " + publishedQuiz.getTitle(),
                    "A new quiz \"" + publishedQuiz.getTitle() + "\" has been assigned to you. "
                            + publishedQuiz.getQuestionCount() + " questions, " + publishedQuiz.getDurationMinutes()
                            + " minutes, passing score " + publishedQuiz.getPassingPercentage() + "%.",
                    "TRAINER");
        }

        audit(getTrainer(trainerEmail), "QUIZ_PUBLISHED",
                "Published quiz '" + publishedQuiz.getTitle() + "' to " + batch.getStudentIds().size() + " students in batch " + batch.getName());
        return publishedQuiz;
    }
    public Page<Quiz> list(String trainerEmail, Pageable pageable) {
        return quizRepository.findByTrainerId(getTrainer(trainerEmail).getId(), pageable);
    }

    public Quiz getOne(String trainerEmail, String quizId) {
        return getOwnedQuiz(trainerEmail, quizId);
    }

    // ==================== helpers ====================

    private Quiz.QuizQuestionEntry findEntry(Quiz quiz, int order) {
        return quiz.getQuestions().stream()
                .filter(e -> e.getOrder() == order)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No question at position " + order));
    }

    private Quiz getOwnedDraftQuiz(String trainerEmail, String quizId) {
        Quiz quiz = getOwnedQuiz(trainerEmail, quizId);
        if (quiz.getStatus() != QuizStatus.DRAFT) {
            throw new BadRequestException("Only a DRAFT quiz can be edited — this one is " + quiz.getStatus());
        }
        return quiz;
    }

    private Quiz getOwnedQuiz(String trainerEmail, String quizId) {
        User trainer = getTrainer(trainerEmail);
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found: " + quizId));
        if (!quiz.getTrainerId().equals(trainer.getId())) {
            throw new BadRequestException("You can only manage quizzes you created");
        }
        return quiz;
    }

    private void verifyBatchOwnership(String trainerId, String batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));
        boolean isMine = trainerId.equals(batch.getTechnicalTrainerId()) || trainerId.equals(batch.getSoftSkillTrainerId());
        if (!isMine) {
            throw new BadRequestException("You are not assigned to batch: " + batch.getName());
        }
    }

    private User getTrainer(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer account not found"));
        if (user.getRole() != Role.TRAINER) {
            throw new BadRequestException("Only trainers can perform this action");
        }
        return user;
    }

    private void audit(User trainer, String action, String details) {
        auditLogService.log(trainer.getId(), trainer.getEmail(), "TRAINER", action, details, null);
    }
}
