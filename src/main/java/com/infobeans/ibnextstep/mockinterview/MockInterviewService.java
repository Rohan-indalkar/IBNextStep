package com.infobeans.ibnextstep.mockinterview;

import com.infobeans.ibnextstep.audit.AuditLogService;
import com.infobeans.ibnextstep.batch.Batch;
import com.infobeans.ibnextstep.batch.BatchRepository;
import com.infobeans.ibnextstep.common.PagedResponse;
import com.infobeans.ibnextstep.common.exception.BadRequestException;
import com.infobeans.ibnextstep.common.exception.ResourceNotFoundException;
import com.infobeans.ibnextstep.common.util.EmailService;
import com.infobeans.ibnextstep.mockinterview.dto.CancelMockInterviewRequest;
import com.infobeans.ibnextstep.mockinterview.dto.CreateMockInterviewRequest;
import com.infobeans.ibnextstep.mockinterview.dto.MockInterviewAnalyticsResponse;
import com.infobeans.ibnextstep.mockinterview.dto.MockInterviewEvaluationRequest;
import com.infobeans.ibnextstep.mockinterview.dto.MockInterviewResponse;
import com.infobeans.ibnextstep.mockinterview.dto.RescheduleMockInterviewRequest;
import com.infobeans.ibnextstep.notification.Notification;
import com.infobeans.ibnextstep.notification.NotificationRepository;
import com.infobeans.ibnextstep.notification.WebPushService;
import com.infobeans.ibnextstep.user.Role;
import com.infobeans.ibnextstep.user.TrainerType;
import com.infobeans.ibnextstep.user.User;
import com.infobeans.ibnextstep.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MockInterviewService {

    private final MockInterviewRepository mockInterviewRepository;
    private final UserRepository userRepository;
    private final BatchRepository batchRepository;
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebPushService webPushService;
    private final AuditLogService auditLogService;

    // ==================== CREATE ====================

    /** Fans out into one MockInterview per selected student; each gets its own invite. */
    public List<MockInterviewResponse> create(String trainerEmail, CreateMockInterviewRequest request) {
        User trainer = getTrainer(trainerEmail);
        Batch batch = getBatchOrThrow(request.getBatchId());
        validateTrainerCanConduct(trainer, batch, request.getInterviewType());

        List<User> students = resolveAndValidateStudents(request.getStudentIds(), batch);

        String meetingLink = (request.getMeetingLink() != null && !request.getMeetingLink().isBlank())
                ? request.getMeetingLink()
                : generateMeetingLink();

        int duration = request.getDurationMinutes() != null ? request.getDurationMinutes() : 30;

        List<MockInterviewResponse> results = new ArrayList<>();
        for (User student : students) {
            MockInterview interview = MockInterview.builder()
                    .batchId(batch.getId())
                    .studentId(student.getId())
                    .studentName(student.getFirstName() + " " + student.getLastName())
                    .trainerId(trainer.getId())
                    .trainerName(trainer.getFirstName() + " " + trainer.getLastName())
                    .interviewType(request.getInterviewType())
                    .scheduledAt(request.getScheduledAt())
                    .durationMinutes(duration)
                    .meetingLink(meetingLink)
                    .notes(request.getNotes())
                    .status(MockInterviewStatus.SCHEDULED)
                    .build();

            interview = mockInterviewRepository.save(interview);

            notify(student, "Mock Interview Scheduled",
                    "Your " + request.getInterviewType() + " mock interview with " + interview.getTrainerName()
                            + " is scheduled on " + interview.getScheduledAt() + ". Meeting link: " + meetingLink
                            + (request.getNotes() != null && !request.getNotes().isBlank() ? ("\n\nNote: " + request.getNotes()) : ""));

            results.add(enrich(interview));
        }

        audit(trainer, "MOCK_INTERVIEW_SCHEDULED",
                "Scheduled " + students.size() + " " + request.getInterviewType() + " mock interview(s) for batch " + batch.getName());

        return results;
    }

    // ==================== RESCHEDULE / CANCEL ====================

    public MockInterviewResponse reschedule(String trainerEmail, String id, RescheduleMockInterviewRequest request) {
        User trainer = getTrainer(trainerEmail);
        MockInterview interview = getOwnedOrThrow(id, trainer);

        if (interview.getStatus() != MockInterviewStatus.SCHEDULED) {
            throw new BadRequestException("Only a SCHEDULED interview can be rescheduled");
        }

        interview.setScheduledAt(request.getScheduledAt());
        if (request.getDurationMinutes() != null) interview.setDurationMinutes(request.getDurationMinutes());
        if (request.getMeetingLink() != null && !request.getMeetingLink().isBlank()) interview.setMeetingLink(request.getMeetingLink());
        if (request.getNotes() != null) interview.setNotes(request.getNotes());

        interview = mockInterviewRepository.save(interview);

        User student = getStudentOrThrow(interview.getStudentId());
        notify(student, "Mock Interview Rescheduled",
                "Your " + interview.getInterviewType() + " mock interview has been rescheduled to "
                        + interview.getScheduledAt() + ". Meeting link: " + interview.getMeetingLink());

        audit(trainer, "MOCK_INTERVIEW_RESCHEDULED", "Rescheduled interview for " + interview.getStudentName());
        return enrich(interview);
    }

    public MockInterviewResponse cancel(String trainerEmail, String id, CancelMockInterviewRequest request) {
        User trainer = getTrainer(trainerEmail);
        MockInterview interview = getOwnedOrThrow(id, trainer);

        if (interview.getStatus() == MockInterviewStatus.PUBLISHED || interview.getStatus() == MockInterviewStatus.CANCELLED) {
            throw new BadRequestException("Cannot cancel an interview that is already " + interview.getStatus());
        }

        interview.setStatus(MockInterviewStatus.CANCELLED);
        interview.setCancellationReason(request.getReason());
        interview = mockInterviewRepository.save(interview);

        User student = getStudentOrThrow(interview.getStudentId());
        notify(student, "Mock Interview Cancelled",
                "Your " + interview.getInterviewType() + " mock interview scheduled on " + interview.getScheduledAt()
                        + " has been cancelled. Reason: " + request.getReason());

        audit(trainer, "MOCK_INTERVIEW_CANCELLED", "Cancelled interview for " + interview.getStudentName() + ": " + request.getReason());
        return enrich(interview);
    }

    // ==================== CONDUCT / EVALUATE / PUBLISH ====================

    public MockInterviewResponse markConducted(String trainerEmail, String id) {
        User trainer = getTrainer(trainerEmail);
        MockInterview interview = getOwnedOrThrow(id, trainer);

        if (interview.getStatus() != MockInterviewStatus.SCHEDULED) {
            throw new BadRequestException("Only a SCHEDULED interview can be marked as conducted");
        }
        interview.setStatus(MockInterviewStatus.CONDUCTED);
        interview = mockInterviewRepository.save(interview);
        audit(trainer, "MOCK_INTERVIEW_CONDUCTED", "Marked interview conducted for " + interview.getStudentName());
        return enrich(interview);
    }

    /** Complete Evaluation Form -> Assign Scores -> Strengths/Weaknesses -> Suggestions -> Calculate Overall Rating. */
    public MockInterviewResponse submitEvaluation(String trainerEmail, String id, MockInterviewEvaluationRequest request) {
        User trainer = getTrainer(trainerEmail);
        MockInterview interview = getOwnedOrThrow(id, trainer);

        if (interview.getStatus() == MockInterviewStatus.CANCELLED || interview.getStatus() == MockInterviewStatus.PUBLISHED) {
            throw new BadRequestException("Cannot evaluate an interview that is " + interview.getStatus());
        }
        // Trainer can submit the evaluation straight after the slot passes even if
        // they forgot to click "mark conducted" first.
        if (interview.getStatus() == MockInterviewStatus.SCHEDULED) {
            interview.setStatus(MockInterviewStatus.CONDUCTED);
        }

        for (Integer score : request.getScores().values()) {
            if (score == null || score < 0 || score > 10) {
                throw new BadRequestException("Each score must be between 0 and 10");
            }
        }
        double overallRating = request.getScores().values().stream()
                .mapToInt(Integer::intValue).average().orElse(0.0);
        overallRating = Math.round(overallRating * 10.0) / 10.0;

        MockInterview.Evaluation evaluation = MockInterview.Evaluation.builder()
                .scores(request.getScores())
                .strengths(request.getStrengths() == null ? List.of() : request.getStrengths())
                .weaknesses(request.getWeaknesses() == null ? List.of() : request.getWeaknesses())
                .improvementSuggestions(request.getImprovementSuggestions() == null ? List.of() : request.getImprovementSuggestions())
                .additionalComments(request.getAdditionalComments())
                .overallRating(overallRating)
                .evaluatedAt(Instant.now())
                .evaluatedByTrainerId(trainer.getId())
                .build();

        interview.setEvaluation(evaluation);
        interview.setStatus(MockInterviewStatus.EVALUATED);
        interview = mockInterviewRepository.save(interview);

        audit(trainer, "MOCK_INTERVIEW_EVALUATED",
                "Submitted evaluation for " + interview.getStudentName() + " (overall rating " + overallRating + "/10)");
        return enrich(interview);
    }

    /** Publish Evaluation Report -> Update Student Performance Dashboard -> Update Placement Readiness Score. */
    public MockInterviewResponse publish(String trainerEmail, String id) {
        User trainer = getTrainer(trainerEmail);
        MockInterview interview = getOwnedOrThrow(id, trainer);

        if (interview.getStatus() != MockInterviewStatus.EVALUATED) {
            throw new BadRequestException("Submit the evaluation before publishing");
        }

        interview.setStatus(MockInterviewStatus.PUBLISHED);
        interview.setPublishedAt(Instant.now());
        interview = mockInterviewRepository.save(interview);

        User student = getStudentOrThrow(interview.getStudentId());
        notify(student, "Mock Interview Feedback Published",
                "Your evaluation report for the " + interview.getInterviewType() + " mock interview is ready. "
                        + "Overall rating: " + interview.getEvaluation().getOverallRating() + "/10.");

        recomputePlacementReadinessScore(student.getId());

        audit(trainer, "MOCK_INTERVIEW_PUBLISHED", "Published evaluation report for " + interview.getStudentName());
        return enrich(interview);
    }

    // ==================== READ / SEARCH ====================

    public MockInterviewResponse getOne(String trainerEmail, String id) {
        User trainer = getTrainer(trainerEmail);
        return enrich(getOwnedOrThrow(id, trainer));
    }

    public PagedResponse<MockInterviewResponse> search(String trainerEmail, MockInterviewSearchCriteria criteria, Pageable pageable) {
        User trainer = getTrainer(trainerEmail);
        criteria.setTrainerId(trainer.getId());
        var page = mockInterviewRepository.search(criteria, pageable);
        return new PagedResponse<>(
                page.getContent().stream().map(this::enrich).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    // ==================== ANALYTICS / REPORTS ====================

    public MockInterviewAnalyticsResponse analytics(String trainerEmail, String batchId, String studentId) {
        User trainer = getTrainer(trainerEmail);
        MockInterviewSearchCriteria criteria = MockInterviewSearchCriteria.builder()
                .trainerId(trainer.getId()).batchId(batchId).studentId(studentId).build();
        List<MockInterview> all = mockInterviewRepository.search(criteria, org.springframework.data.domain.Pageable.unpaged()).getContent();

        long scheduled = all.stream().filter(m -> m.getStatus() == MockInterviewStatus.SCHEDULED).count();
        long conducted = all.stream().filter(m -> m.getStatus() == MockInterviewStatus.CONDUCTED).count();
        long evaluated = all.stream().filter(m -> m.getStatus() == MockInterviewStatus.EVALUATED).count();
        long cancelled = all.stream().filter(m -> m.getStatus() == MockInterviewStatus.CANCELLED).count();

        List<MockInterview> published = all.stream().filter(m -> m.getStatus() == MockInterviewStatus.PUBLISHED).toList();

        java.util.OptionalDouble avgOverallOpt = published.stream()
                .map(m -> m.getEvaluation().getOverallRating())
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue).average();
        Double avgOverall = avgOverallOpt.isPresent() ? avgOverallOpt.getAsDouble() : null;

        Map<String, Double> avgByType = new HashMap<>();
        for (InterviewType type : InterviewType.values()) {
            var ratings = published.stream()
                    .filter(m -> m.getInterviewType() == type)
                    .map(m -> m.getEvaluation().getOverallRating())
                    .filter(Objects::nonNull)
                    .toList();
            if (!ratings.isEmpty()) {
                avgByType.put(type.name(), round1(ratings.stream().mapToDouble(Double::doubleValue).average().orElse(0.0)));
            }
        }

        Map<String, List<Integer>> paramScores = new HashMap<>();
        for (MockInterview m : published) {
            if (m.getEvaluation() == null || m.getEvaluation().getScores() == null) continue;
            m.getEvaluation().getScores().forEach((param, score) ->
                    paramScores.computeIfAbsent(param, k -> new ArrayList<>()).add(score));
        }
        Map<String, Double> avgByParam = new HashMap<>();
        paramScores.forEach((param, scores) ->
                avgByParam.put(param, round1(scores.stream().mapToInt(Integer::intValue).average().orElse(0.0))));

        return MockInterviewAnalyticsResponse.builder()
                .totalScheduled(scheduled)
                .totalConducted(conducted)
                .totalEvaluated(evaluated)
                .totalPublished(published.size())
                .totalCancelled(cancelled)
                .averageOverallRating(avgOverall != null ? round1(avgOverall) : null)
                .averageRatingByInterviewType(avgByType)
                .averageScoreByParameter(avgByParam)
                .build();
    }

    // ==================== helpers ====================

    private void recomputePlacementReadinessScore(String studentId) {
        List<MockInterview> published = mockInterviewRepository.findByStudentIdAndStatus(studentId, MockInterviewStatus.PUBLISHED);
        double avgOutOf10 = published.stream()
                .map(m -> m.getEvaluation() != null ? m.getEvaluation().getOverallRating() : null)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue).average().orElse(0.0);

        User student = getStudentOrThrow(studentId);
        // Scaled to a 0-100 readiness score. Currently sourced from mock
        // interviews only; will blend in quiz/assignment scores once those
        // modules publish results too.
        student.setPlacementReadinessScore(round1(avgOutOf10 * 10));
        student.setPlacementReadinessScoreUpdatedAt(Instant.now());
        userRepository.save(student);
    }

    private String generateMeetingLink() {
        return "https://meet.jit.si/IBNextStep-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private List<User> resolveAndValidateStudents(List<String> studentIds, Batch batch) {
        List<User> students = new ArrayList<>();
        for (String studentId : studentIds) {
            User student = userRepository.findById(studentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
            if (student.getRole() != Role.STUDENT) {
                throw new BadRequestException(student.getEmail() + " is not a student account");
            }
            if (batch.getStudentIds() == null || !batch.getStudentIds().contains(studentId)) {
                throw new BadRequestException(student.getEmail() + " is not part of batch " + batch.getName());
            }
            students.add(student);
        }
        return students;
    }

    private void validateTrainerCanConduct(User trainer, Batch batch, InterviewType type) {
        if (trainer.getTrainerType() != type.getRequiredTrainerType()) {
            throw new BadRequestException("A " + type + " interview must be conducted by a " + type.getRequiredTrainerType() + " trainer");
        }
        String assignedTrainerId = type.getRequiredTrainerType() == TrainerType.TECHNICAL
                ? batch.getTechnicalTrainerId() : batch.getSoftSkillTrainerId();
        if (assignedTrainerId == null || !assignedTrainerId.equals(trainer.getId())) {
            throw new BadRequestException("You are not the assigned " + type.getRequiredTrainerType() + " trainer for batch " + batch.getName());
        }
    }

    private void notify(User recipient, String title, String message) {
        Notification notification = Notification.builder()
                .recipientUserId(recipient.getId())
                .title(title)
                .message(message)
                .senderRole("TRAINER")
                .read(false)
                .createdAt(Instant.now())
                .build();
        notification = notificationRepository.save(notification);

        emailService.send(recipient.getEmail(), title, message);
        messagingTemplate.convertAndSendToUser(recipient.getEmail(), "/queue/notifications", notification);
        webPushService.sendToUser(recipient.getId(), title, message);
    }

    private void audit(User trainer, String action, String details) {
        auditLogService.log(trainer.getId(), trainer.getEmail(), "TRAINER", action, details, null);
    }

    private User getTrainer(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.TRAINER) {
            throw new BadRequestException("Only trainers can manage mock interviews");
        }
        return user;
    }

    private User getStudentOrThrow(String studentId) {
        return userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
    }

    private Batch getBatchOrThrow(String batchId) {
        return batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));
    }

    private MockInterview getOwnedOrThrow(String id, User trainer) {
        MockInterview interview = mockInterviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mock interview not found: " + id));
        if (!interview.getTrainerId().equals(trainer.getId())) {
            throw new BadRequestException("You can only manage mock interviews you scheduled");
        }
        return interview;
    }

    private MockInterviewResponse enrich(MockInterview interview) {
        var builder = MockInterviewResponse.fromEntity(interview);
        batchRepository.findById(interview.getBatchId()).map(Batch::getName).ifPresent(builder::batchName);
        return builder.build();
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
