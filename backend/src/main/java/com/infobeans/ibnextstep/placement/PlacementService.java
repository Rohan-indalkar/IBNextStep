package com.infobeans.ibnextstep.placement;

import com.infobeans.ibnextstep.audit.AuditLogService;
import com.infobeans.ibnextstep.common.PagedResponse;
import com.infobeans.ibnextstep.common.exception.BadRequestException;
import com.infobeans.ibnextstep.common.exception.ResourceNotFoundException;
import com.infobeans.ibnextstep.common.util.FileStorageService;
import com.infobeans.ibnextstep.notification.NotificationService;
import com.infobeans.ibnextstep.placement.dto.CreatePlacementRequest;
import com.infobeans.ibnextstep.placement.dto.EligibilityCheckResponse;
import com.infobeans.ibnextstep.placement.dto.EligibilityCriteriaDto;
import com.infobeans.ibnextstep.placement.dto.PlacementResponse;
import com.infobeans.ibnextstep.placement.dto.RoundTemplateDto;
import com.infobeans.ibnextstep.placement.dto.StudentPlacementResponse;
import com.infobeans.ibnextstep.placement.dto.UpdatePlacementRequest;
import com.infobeans.ibnextstep.user.Role;
import com.infobeans.ibnextstep.user.User;
import com.infobeans.ibnextstep.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlacementService {

    private final PlacementRepository placementRepository;
    private final PlacementApplicationRepository applicationRepository;
    private final CompanyService companyService;
    private final PlacementEligibilityService eligibilityService;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    // ---------- HR: authoring ----------

    public PlacementResponse create(User hr, CreatePlacementRequest request) {
        Company company = companyService.getCompanyOrThrow(request.getCompanyId());
        if (!company.isActive()) {
            throw new BadRequestException("Cannot create a placement drive against an inactive company");
        }

        if (request.getType() == PlacementType.OFF_CAMPUS && request.getEligibility() != null) {
            throw new BadRequestException("Off-campus placements do not use eligibility criteria");
        }

        Placement placement = Placement.builder()
                .companyId(company.getId())
                .companyName(company.getName())
                .companyLogoPath(company.getLogoPath())
                .title(request.getTitle())
                .description(request.getDescription())
                .type(request.getType())
                .status(PlacementStatus.DRAFT)
                .eligibility(toEntity(request.getEligibility()))
                .applicationDeadline(request.getApplicationDeadline())
                .externalApplyLink(request.getExternalApplyLink())
                .interviewRoundTemplates(toTemplateEntities(request.getInterviewRoundTemplates()))
                .packageLpa(request.getPackageLpa())
                .createdByHrId(hr.getId())
                .createdByHrName(fullName(hr))
                .build();
        placement = placementRepository.save(placement);

        auditLogService.log(hr.getId(), hr.getEmail(), hr.getRole().name(), "PLACEMENT_CREATED",
                "Created " + placement.getType() + " placement '" + placement.getTitle() + "' for " + company.getName(), null);

        return toResponse(placement);
    }

    public PlacementResponse update(User hr, String placementId, UpdatePlacementRequest request) {
        Placement placement = getPlacementOrThrow(placementId);
        assertEditable(placement);

        if (placement.getType() == PlacementType.OFF_CAMPUS && request.getEligibility() != null) {
            throw new BadRequestException("Off-campus placements do not use eligibility criteria");
        }

        placement.setTitle(request.getTitle());
        placement.setDescription(request.getDescription());
        placement.setEligibility(toEntity(request.getEligibility()));
        placement.setApplicationDeadline(request.getApplicationDeadline());
        placement.setExternalApplyLink(request.getExternalApplyLink());
        placement.setInterviewRoundTemplates(toTemplateEntities(request.getInterviewRoundTemplates()));
        placement.setPackageLpa(request.getPackageLpa());
        placement = placementRepository.save(placement);

        auditLogService.log(hr.getId(), hr.getEmail(), hr.getRole().name(), "PLACEMENT_UPDATED",
                "Updated placement '" + placement.getTitle() + "'", null);

        return toResponse(placement);
    }

    public PlacementResponse uploadPdf(User hr, String placementId, MultipartFile file) {
        Placement placement = getPlacementOrThrow(placementId);
        assertEditable(placement);

        String oldPath = placement.getPdfPath();
        String path = fileStorageService.store(file, "placement-brochures");
        placement.setPdfPath(path);
        placement.setPdfFileName(file.getOriginalFilename());
        placement = placementRepository.save(placement);

        if (oldPath != null) {
            fileStorageService.delete(oldPath);
        }

        auditLogService.log(hr.getId(), hr.getEmail(), hr.getRole().name(), "PLACEMENT_PDF_UPLOADED",
                "Uploaded brochure for placement '" + placement.getTitle() + "'", null);

        return toResponse(placement);
    }

    public PlacementResponse publish(User hr, String placementId) {
        Placement placement = getPlacementOrThrow(placementId);
        if (placement.getStatus() == PlacementStatus.PUBLISHED) {
            throw new BadRequestException("Placement is already published");
        }
        if (placement.getStatus() == PlacementStatus.CLOSED) {
            throw new BadRequestException("Cannot publish a closed placement");
        }
        if (placement.getPdfPath() == null) {
            throw new BadRequestException("Upload the placement PDF before publishing");
        }

        placement.setStatus(PlacementStatus.PUBLISHED);
        placement.setPublishedAt(Instant.now());
        placement = placementRepository.save(placement);

        auditLogService.log(hr.getId(), hr.getEmail(), hr.getRole().name(), "PLACEMENT_PUBLISHED",
                "Published placement '" + placement.getTitle() + "'", null);

        notifyAllStudents("New placement published: " + placement.getTitle(),
                placement.getCompanyName() + " — " + placement.getTitle() + " is now open. Check it out on your placements page.");

        return toResponse(placement);
    }

    public PlacementResponse close(User hr, String placementId) {
        Placement placement = getPlacementOrThrow(placementId);
        if (placement.getStatus() != PlacementStatus.PUBLISHED) {
            throw new BadRequestException("Only a published placement can be closed");
        }

        placement.setStatus(PlacementStatus.CLOSED);
        placement.setClosedAt(Instant.now());
        placement = placementRepository.save(placement);

        auditLogService.log(hr.getId(), hr.getEmail(), hr.getRole().name(), "PLACEMENT_CLOSED",
                "Closed placement '" + placement.getTitle() + "'", null);

        notifyApplicantsOfClosure(placement);

        return toResponse(placement);
    }

    public PagedResponse<PlacementResponse> searchForHr(PlacementStatus status, PlacementType type, Pageable pageable) {
        Page<Placement> page = status != null && type != null
                ? placementRepository.findByStatusAndType(status, type, pageable)
                : status != null
                        ? placementRepository.findByStatus(status, pageable)
                        : placementRepository.findAll(pageable);
        return PagedResponse.from(page.map(this::toResponseWithCounts));
    }

    public PlacementResponse getForHr(String placementId) {
        return toResponseWithCounts(getPlacementOrThrow(placementId));
    }

    // ---------- Student: browse ----------

    public PagedResponse<StudentPlacementResponse> browseForStudent(String studentId, Pageable pageable) {
        Page<Placement> page = placementRepository.findByStatus(PlacementStatus.PUBLISHED, pageable);
        return PagedResponse.from(page.map(p -> toStudentResponse(p, studentId)));
    }

    public StudentPlacementResponse getForStudent(String studentId, String placementId) {
        Placement placement = getPlacementOrThrow(placementId);
        if (placement.getStatus() != PlacementStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Placement not found: " + placementId);
        }
        return toStudentResponse(placement, studentId);
    }

    /** Enforces the same eligibility gate as the browse view before handing back the file path to stream. */
    public Placement getForPdfDownload(String studentId, String placementId) {
        Placement placement = getPlacementOrThrow(placementId);
        if (placement.getStatus() != PlacementStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Placement not found: " + placementId);
        }
        if (placement.getPdfPath() == null) {
            throw new ResourceNotFoundException("No brochure uploaded for this placement yet");
        }
        if (placement.getType() == PlacementType.CAMPUS) {
            EligibilityCheckResponse eligibility = eligibilityService.check(studentId, placement.getEligibility());
            if (!eligibility.isEligible()) {
                throw new BadRequestException("You must be eligible for this placement to download its brochure");
            }
        }
        return placement;
    }

    // ---------- internal helpers (also used by PlacementApplicationService) ----------

    Placement getPlacementOrThrow(String placementId) {
        return placementRepository.findById(placementId)
                .orElseThrow(() -> new ResourceNotFoundException("Placement not found: " + placementId));
    }

    private void assertEditable(Placement placement) {
        if (placement.getStatus() == PlacementStatus.CLOSED) {
            throw new BadRequestException("Cannot edit a closed placement");
        }
    }

    private StudentPlacementResponse toStudentResponse(Placement placement, String studentId) {
        EligibilityCheckResponse eligibility = placement.getType() == PlacementType.CAMPUS
                ? eligibilityService.check(studentId, placement.getEligibility())
                : EligibilityCheckResponse.builder().eligible(true).build();

        var existingApplication = applicationRepository.findByPlacementIdAndStudentId(placement.getId(), studentId);
        boolean applied = existingApplication.isPresent();

        boolean campusGatesApply = placement.getType() == PlacementType.CAMPUS;
        boolean applyEnabled = placement.getType() == PlacementType.OFF_CAMPUS
                ? false // off-campus never creates an internal application — student uses the external link instead
                : !applied && eligibility.isEligible() && !isPastDeadline(placement);
        boolean pdfDownloadEnabled = !campusGatesApply || eligibility.isEligible();

        return StudentPlacementResponse.builder()
                .id(placement.getId())
                .companyName(placement.getCompanyName())
                .companyLogoPath(placement.getCompanyLogoPath())
                .title(placement.getTitle())
                .description(placement.getDescription())
                .type(placement.getType())
                .applicationDeadline(placement.getApplicationDeadline())
                .pdfFileName(placement.getPdfFileName())
                .externalApplyLink(placement.getExternalApplyLink())
                .packageLpa(placement.getPackageLpa())
                .eligible(eligibility.isEligible())
                .eligibility(eligibility)
                .applyEnabled(applyEnabled)
                .pdfDownloadEnabled(pdfDownloadEnabled)
                .alreadyApplied(applied)
                .myApplicationStatus(existingApplication.map(PlacementApplication::getStatus).orElse(null))
                .build();
    }

    private boolean isPastDeadline(Placement placement) {
        return placement.getApplicationDeadline() != null && Instant.now().isAfter(placement.getApplicationDeadline());
    }

    private void notifyAllStudents(String title, String message) {
        List<User> students = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.STUDENT)
                .toList();
        for (User student : students) {
            notificationService.sendToUser(student.getId(), title, message, Role.HR.name());
        }
    }

    private void notifyApplicantsOfClosure(Placement placement) {
        for (PlacementApplication app : findApplicationsForPlacement(placement.getId())) {
            notificationService.sendToUser(app.getStudentId(), "Placement closed",
                    placement.getCompanyName() + " — " + placement.getTitle() + " has been closed.", Role.HR.name());
        }
    }

    private List<PlacementApplication> findApplicationsForPlacement(String placementId) {
        return applicationRepository.search(
                        PlacementApplicationSearchCriteria.builder().placementId(placementId).build(),
                        Pageable.unpaged())
                .getContent();
    }

    private Placement.EligibilityCriteria toEntity(EligibilityCriteriaDto dto) {
        if (dto == null) {
            return null;
        }
        return Placement.EligibilityCriteria.builder()
                .minAttendancePercentage(dto.getMinAttendancePercentage())
                .minQuizPercentage(dto.getMinQuizPercentage())
                .minCodingPercentage(dto.getMinCodingPercentage())
                .minMockInterviewRating(dto.getMinMockInterviewRating())
                .minStudentEvaluationScore(dto.getMinStudentEvaluationScore())
                .requireResumeApproved(dto.getRequireResumeApproved())
                .build();
    }

    private EligibilityCriteriaDto toDto(Placement.EligibilityCriteria e) {
        if (e == null) {
            return null;
        }
        EligibilityCriteriaDto dto = new EligibilityCriteriaDto();
        dto.setMinAttendancePercentage(e.getMinAttendancePercentage());
        dto.setMinQuizPercentage(e.getMinQuizPercentage());
        dto.setMinCodingPercentage(e.getMinCodingPercentage());
        dto.setMinMockInterviewRating(e.getMinMockInterviewRating());
        dto.setMinStudentEvaluationScore(e.getMinStudentEvaluationScore());
        dto.setRequireResumeApproved(e.getRequireResumeApproved());
        return dto;
    }

    private List<Placement.RoundTemplate> toTemplateEntities(List<RoundTemplateDto> dtos) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream()
                .map(d -> Placement.RoundTemplate.builder().roundNumber(d.getRoundNumber()).name(d.getName()).build())
                .toList();
    }

    private List<RoundTemplateDto> toTemplateDtos(List<Placement.RoundTemplate> templates) {
        if (templates == null) {
            return List.of();
        }
        return templates.stream().map(t -> {
            RoundTemplateDto dto = new RoundTemplateDto();
            dto.setRoundNumber(t.getRoundNumber());
            dto.setName(t.getName());
            return dto;
        }).toList();
    }

    private String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }

    PlacementResponse toResponse(Placement p) {
        return PlacementResponse.builder()
                .id(p.getId())
                .companyId(p.getCompanyId())
                .companyName(p.getCompanyName())
                .companyLogoPath(p.getCompanyLogoPath())
                .title(p.getTitle())
                .description(p.getDescription())
                .type(p.getType())
                .status(p.getStatus())
                .eligibility(toDto(p.getEligibility()))
                .applicationDeadline(p.getApplicationDeadline())
                .pdfPath(p.getPdfPath())
                .pdfFileName(p.getPdfFileName())
                .externalApplyLink(p.getExternalApplyLink())
                .interviewRoundTemplates(toTemplateDtos(p.getInterviewRoundTemplates()))
                .packageLpa(p.getPackageLpa())
                .createdByHrName(p.getCreatedByHrName())
                .publishedAt(p.getPublishedAt())
                .closedAt(p.getClosedAt())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private PlacementResponse toResponseWithCounts(Placement p) {
        PlacementResponse response = toResponse(p);
        response.setTotalApplications(applicationRepository.countByPlacementId(p.getId()));
        response.setShortlisted(applicationRepository.countByPlacementIdAndStatus(p.getId(), PlacementApplicationStatus.SHORTLISTED));
        response.setSelected(applicationRepository.countByPlacementIdAndStatus(p.getId(), PlacementApplicationStatus.SELECTED));
        return response;
    }
}
