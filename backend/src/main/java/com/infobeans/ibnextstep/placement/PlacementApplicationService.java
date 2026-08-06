package com.infobeans.ibnextstep.placement;

import com.infobeans.ibnextstep.audit.AuditLogService;
import com.infobeans.ibnextstep.common.PagedResponse;
import com.infobeans.ibnextstep.common.exception.BadRequestException;
import com.infobeans.ibnextstep.common.exception.ResourceNotFoundException;
import com.infobeans.ibnextstep.department.Department;
import com.infobeans.ibnextstep.department.DepartmentRepository;
import com.infobeans.ibnextstep.notification.NotificationService;
import com.infobeans.ibnextstep.placement.dto.EligibilityCheckResponse;
import com.infobeans.ibnextstep.placement.dto.InterviewRoundDto;
import com.infobeans.ibnextstep.placement.dto.PlacementApplicationResponse;
import com.infobeans.ibnextstep.placement.dto.RejectApplicationRequest;
import com.infobeans.ibnextstep.placement.dto.RescheduleInterviewRequest;
import com.infobeans.ibnextstep.placement.dto.ScheduleInterviewRequest;
import com.infobeans.ibnextstep.placement.dto.UpdateInterviewResultRequest;
import com.infobeans.ibnextstep.placement.exception.NotEligibleException;
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

@Service
@RequiredArgsConstructor
public class PlacementApplicationService {

    private final PlacementApplicationRepository applicationRepository;
    private final PlacementRepository placementRepository;
    private final PlacementEligibilityService eligibilityService;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    // ---------- Student: apply ----------

    public PlacementApplicationResponse apply(User student, String placementId) {
        Placement placement = placementRepository.findById(placementId)
                .orElseThrow(() -> new ResourceNotFoundException("Placement not found: " + placementId));

        if (placement.getType() != PlacementType.CAMPUS) {
            throw new BadRequestException("Off-campus placements don't take internal applications — use the external apply link");
        }
        if (placement.getStatus() != PlacementStatus.PUBLISHED) {
            throw new BadRequestException("This placement is not currently open for applications");
        }
        if (placement.getApplicationDeadline() != null && Instant.now().isAfter(placement.getApplicationDeadline())) {
            throw new BadRequestException("The application deadline for this placement has passed");
        }
        if (applicationRepository.existsByPlacementIdAndStudentId(placementId, student.getId())) {
            throw new BadRequestException("You have already applied to this placement");
        }

        // Revalidate — never trust the eligibility the student saw on an earlier screen.
        EligibilityCheckResponse eligibility = eligibilityService.check(student.getId(), placement.getEligibility());
        if (!eligibility.isEligible()) {
            notifyStudentNotEligible(student, placement, eligibility);
            throw new NotEligibleException(eligibility);
        }

        String departmentId = student.getDepartmentId();
        String departmentName = departmentId == null ? null
                : departmentRepository.findById(departmentId).map(Department::getName).orElse(null);

        PlacementApplication application = PlacementApplication.builder()
                .placementId(placement.getId())
                .companyId(placement.getCompanyId())
                .companyName(placement.getCompanyName())
                .placementTitle(placement.getTitle())
                .studentId(student.getId())
                .studentName(fullName(student))
                .studentEmail(student.getEmail())
                .departmentId(departmentId)
                .departmentName(departmentName)
                .status(PlacementApplicationStatus.APPLIED)
                .currentRoundIndex(-1)
                .appliedAt(Instant.now())
                .build();
        application = applicationRepository.save(application);

        auditLogService.log(student.getId(), student.getEmail(), student.getRole().name(), "PLACEMENT_APPLICATION_SUBMITTED",
                "Applied to placement '" + placement.getTitle() + "' at " + placement.getCompanyName(), null);

        notifyHrOfNewApplication(application);
        notifyStudentApplicationSubmitted(student, application);

        return toResponse(application);
    }

    public List<PlacementApplicationResponse> getMyApplications(String studentId) {
        return applicationRepository.findByStudentId(studentId).stream()
                .map(this::toResponse)
                .toList();
    }

    // ---------- HR: pipeline ----------

    public PagedResponse<PlacementApplicationResponse> search(PlacementApplicationSearchCriteria criteria, Pageable pageable) {
        return PagedResponse.from(applicationRepository.search(criteria, pageable).map(this::toResponse));
    }

    public PlacementApplicationResponse getOne(String applicationId) {
        return toResponse(getApplicationOrThrow(applicationId));
    }

    public PlacementApplicationResponse shortlist(User hr, String applicationId) {
        PlacementApplication application = getApplicationOrThrow(applicationId);
        assertStatus(application, PlacementApplicationStatus.APPLIED);

        application.setStatus(PlacementApplicationStatus.SHORTLISTED);
        application.setShortlistedAt(Instant.now());
        application = applicationRepository.save(application);

        audit(hr, "PLACEMENT_CANDIDATE_SHORTLISTED", application);
        notify(application, "You've been shortlisted!",
                "You've been shortlisted for " + application.getPlacementTitle() + " at " + application.getCompanyName() + ".");

        return toResponse(application);
    }

    public PlacementApplicationResponse reject(User hr, String applicationId, RejectApplicationRequest request) {
        PlacementApplication application = getApplicationOrThrow(applicationId);
        if (application.getStatus() == PlacementApplicationStatus.SELECTED
                || application.getStatus() == PlacementApplicationStatus.REJECTED
                || application.getStatus() == PlacementApplicationStatus.NOT_SELECTED) {
            throw new BadRequestException("This application is already in a final state");
        }

        application.setStatus(PlacementApplicationStatus.REJECTED);
        application.setRejectedAt(Instant.now());
        application.setRejectionReason(request.getReason());
        application = applicationRepository.save(application);

        audit(hr, "PLACEMENT_CANDIDATE_REJECTED", application);
        notify(application, "Application update",
                "Your application for " + application.getPlacementTitle() + " at " + application.getCompanyName()
                        + " was not successful. Reason: " + request.getReason());

        return toResponse(application);
    }

    public PlacementApplicationResponse selectCandidate(User hr, String applicationId) {
        PlacementApplication application = getApplicationOrThrow(applicationId);
        if (application.getStatus() != PlacementApplicationStatus.INTERVIEW_SCHEDULED
                && application.getStatus() != PlacementApplicationStatus.SHORTLISTED) {
            throw new BadRequestException("Only a shortlisted or interviewing candidate can be marked as selected");
        }

        application.setStatus(PlacementApplicationStatus.SELECTED);
        application.setSelectedAt(Instant.now());
        application = applicationRepository.save(application);

        audit(hr, "PLACEMENT_CANDIDATE_SELECTED", application);
        notify(application, "Congratulations — you're selected!",
                "You've been selected for " + application.getPlacementTitle() + " at " + application.getCompanyName() + "!");

        return toResponse(application);
    }

    public PlacementApplicationResponse markNotSelected(User hr, String applicationId, RejectApplicationRequest request) {
        PlacementApplication application = getApplicationOrThrow(applicationId);
        assertStatus(application, PlacementApplicationStatus.INTERVIEW_SCHEDULED);

        application.setStatus(PlacementApplicationStatus.NOT_SELECTED);
        application.setRejectedAt(Instant.now());
        application.setRejectionReason(request.getReason());
        application = applicationRepository.save(application);

        audit(hr, "PLACEMENT_CANDIDATE_NOT_SELECTED", application);
        notify(application, "Application update",
                "Your application for " + application.getPlacementTitle() + " at " + application.getCompanyName()
                        + " was not successful. Reason: " + request.getReason());

        return toResponse(application);
    }

    // ---------- HR: interview round management ----------

    public PlacementApplicationResponse scheduleRound(User hr, String applicationId, ScheduleInterviewRequest request) {
        PlacementApplication application = getApplicationOrThrow(applicationId);
        if (application.getStatus() != PlacementApplicationStatus.SHORTLISTED
                && application.getStatus() != PlacementApplicationStatus.INTERVIEW_SCHEDULED) {
            throw new BadRequestException("Only a shortlisted candidate can have interview rounds scheduled");
        }

        List<PlacementApplication.InterviewRoundInstance> rounds = new ArrayList<>(application.getRounds());
        int roundNumber = rounds.size() + 1;

        PlacementApplication.InterviewRoundInstance round = PlacementApplication.InterviewRoundInstance.builder()
                .roundNumber(roundNumber)
                .roundType(request.getRoundType())
                .scheduledAt(request.getScheduledAt())
                .durationMinutes(request.getDurationMinutes())
                .venue(request.getVenue())
                .meetingLink(request.getMeetingLink())
                .remarks(request.getRemarks())
                .status(InterviewRoundStatus.SCHEDULED)
                .result(InterviewRoundResult.PENDING)
                .updatedAt(Instant.now())
                .build();
        rounds.add(round);

        application.setRounds(rounds);
        application.setCurrentRoundIndex(rounds.size() - 1);
        application.setStatus(PlacementApplicationStatus.INTERVIEW_SCHEDULED);
        application = applicationRepository.save(application);

        audit(hr, "PLACEMENT_INTERVIEW_SCHEDULED",
                application, "Scheduled round " + roundNumber + " (" + request.getRoundType() + ") for");
        notify(application, "Interview scheduled",
                "Round " + roundNumber + " (" + request.getRoundType() + ") for " + application.getPlacementTitle()
                        + " has been scheduled. Check your dashboard for details.");

        return toResponse(application);
    }

    public PlacementApplicationResponse rescheduleRound(User hr, String applicationId, int roundNumber, RescheduleInterviewRequest request) {
        PlacementApplication application = getApplicationOrThrow(applicationId);
        PlacementApplication.InterviewRoundInstance round = getRoundOrThrow(application, roundNumber);
        if (round.getStatus() == InterviewRoundStatus.COMPLETED || round.getStatus() == InterviewRoundStatus.CANCELLED) {
            throw new BadRequestException("Cannot reschedule a " + round.getStatus().name().toLowerCase() + " round");
        }

        round.setScheduledAt(request.getScheduledAt());
        if (request.getDurationMinutes() != null) round.setDurationMinutes(request.getDurationMinutes());
        if (request.getVenue() != null) round.setVenue(request.getVenue());
        if (request.getMeetingLink() != null) round.setMeetingLink(request.getMeetingLink());
        if (request.getRemarks() != null) round.setRemarks(request.getRemarks());
        round.setStatus(InterviewRoundStatus.RESCHEDULED);
        round.setUpdatedAt(Instant.now());
        applicationRepository.save(application);

        audit(hr, "PLACEMENT_INTERVIEW_RESCHEDULED", application, "Rescheduled round " + roundNumber + " for");
        notify(application, "Interview rescheduled",
                "Round " + roundNumber + " for " + application.getPlacementTitle() + " has been rescheduled.");

        return toResponse(application);
    }

    public PlacementApplicationResponse cancelRound(User hr, String applicationId, int roundNumber, RejectApplicationRequest request) {
        PlacementApplication application = getApplicationOrThrow(applicationId);
        PlacementApplication.InterviewRoundInstance round = getRoundOrThrow(application, roundNumber);

        round.setStatus(InterviewRoundStatus.CANCELLED);
        round.setResultRemarks(request.getReason());
        round.setUpdatedAt(Instant.now());
        applicationRepository.save(application);

        audit(hr, "PLACEMENT_INTERVIEW_CANCELLED", application, "Cancelled round " + roundNumber + " for");
        notify(application, "Interview cancelled",
                "Round " + roundNumber + " for " + application.getPlacementTitle() + " has been cancelled. Reason: " + request.getReason());

        return toResponse(application);
    }

    public PlacementApplicationResponse updateRoundResult(User hr, String applicationId, int roundNumber, UpdateInterviewResultRequest request) {
        PlacementApplication application = getApplicationOrThrow(applicationId);
        PlacementApplication.InterviewRoundInstance round = getRoundOrThrow(application, roundNumber);

        round.setStatus(InterviewRoundStatus.COMPLETED);
        round.setResult(request.getResult());
        round.setResultRemarks(request.getResultRemarks());
        round.setUpdatedAt(Instant.now());

        if (request.getResult() == InterviewRoundResult.REJECTED) {
            application.setStatus(PlacementApplicationStatus.NOT_SELECTED);
            application.setRejectedAt(Instant.now());
            application.setRejectionReason("Did not qualify round " + roundNumber
                    + (request.getResultRemarks() != null ? ": " + request.getResultRemarks() : ""));
        }
        applicationRepository.save(application);

        audit(hr, "PLACEMENT_INTERVIEW_RESULT_UPDATED",
                application, "Marked round " + roundNumber + " as " + request.getResult() + " for");
        notify(application, request.getResult() == InterviewRoundResult.QUALIFIED ? "Round qualified" : "Round result",
                "Round " + roundNumber + " for " + application.getPlacementTitle() + ": " + request.getResult()
                        + (request.getResultRemarks() != null ? " — " + request.getResultRemarks() : ""));

        return toResponse(application);
    }

    // ---------- internal helpers ----------

    PlacementApplication getApplicationOrThrow(String applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));
    }

    private PlacementApplication.InterviewRoundInstance getRoundOrThrow(PlacementApplication application, int roundNumber) {
        return application.getRounds().stream()
                .filter(r -> r.getRoundNumber() == roundNumber)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Round " + roundNumber + " not found on this application"));
    }

    private void assertStatus(PlacementApplication application, PlacementApplicationStatus expected) {
        if (application.getStatus() != expected) {
            throw new BadRequestException("Application must be in " + expected + " status for this action (currently "
                    + application.getStatus() + ")");
        }
    }

    private void audit(User hr, String action, PlacementApplication application) {
        audit(hr, action, application, action.replace('_', ' ').toLowerCase() + " for");
    }

    private void audit(User hr, String action, PlacementApplication application, String verbPhrase) {
        auditLogService.log(hr.getId(), hr.getEmail(), hr.getRole().name(), action,
                verbPhrase + " " + application.getStudentName() + " (" + application.getPlacementTitle() + ")", null);
    }

    private void notify(PlacementApplication application, String title, String message) {
        notificationService.sendToUser(application.getStudentId(), title, message, Role.HR.name());
    }

    /** Every active HR user gets notified so a new application doesn't sit unseen until someone happens to check the dashboard. */
    private void notifyHrOfNewApplication(PlacementApplication application) {
        List<User> hrUsers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.HR)
                .toList();
        String title = "New application: " + application.getPlacementTitle();
        String message = application.getStudentName() + " applied to " + application.getPlacementTitle()
                + " at " + application.getCompanyName() + ".";
        for (User hrUser : hrUsers) {
            notificationService.sendToUser(hrUser.getId(), title, message, Role.STUDENT.name());
        }
    }

    /** "Application Submitted" confirmation — sent to the student themselves. */
    private void notifyStudentApplicationSubmitted(User student, PlacementApplication application) {
        notificationService.sendToUser(student.getId(), "Application submitted",
                "Your application for " + application.getPlacementTitle() + " at " + application.getCompanyName()
                        + " has been submitted successfully. You'll be notified as your application progresses.",
                Role.STUDENT.name());
    }

    /** Sent when apply is rejected for ineligibility — lists every failed rule, not just a generic "not eligible". */
    private void notifyStudentNotEligible(User student, Placement placement, EligibilityCheckResponse eligibility) {
        String reasons = eligibility.getFailedCriteria().stream()
                .map(f -> "- " + f.getCriterion() + ": " + f.getReason())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("You do not currently meet the eligibility criteria for this placement.");

        notificationService.sendToUser(student.getId(), "Not eligible: " + placement.getTitle(),
                "Your application for " + placement.getTitle() + " at " + placement.getCompanyName()
                        + " could not be submitted. Reasons:\n" + reasons,
                Role.STUDENT.name());
    }

    private String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }

    private PlacementApplicationResponse toResponse(PlacementApplication a) {
        List<InterviewRoundDto> rounds = a.getRounds().stream()
                .map(r -> InterviewRoundDto.builder()
                        .roundNumber(r.getRoundNumber())
                        .roundType(r.getRoundType())
                        .scheduledAt(r.getScheduledAt())
                        .durationMinutes(r.getDurationMinutes())
                        .venue(r.getVenue())
                        .meetingLink(r.getMeetingLink())
                        .remarks(r.getRemarks())
                        .status(r.getStatus())
                        .result(r.getResult())
                        .resultRemarks(r.getResultRemarks())
                        .updatedAt(r.getUpdatedAt())
                        .build())
                .toList();

        return PlacementApplicationResponse.builder()
                .id(a.getId())
                .placementId(a.getPlacementId())
                .placementTitle(a.getPlacementTitle())
                .companyId(a.getCompanyId())
                .companyName(a.getCompanyName())
                .studentId(a.getStudentId())
                .studentName(a.getStudentName())
                .studentEmail(a.getStudentEmail())
                .departmentName(a.getDepartmentName())
                .status(a.getStatus())
                .rounds(rounds)
                .currentRoundIndex(a.getCurrentRoundIndex())
                .appliedAt(a.getAppliedAt())
                .shortlistedAt(a.getShortlistedAt())
                .rejectedAt(a.getRejectedAt())
                .rejectionReason(a.getRejectionReason())
                .selectedAt(a.getSelectedAt())
                .build();
    }
}
