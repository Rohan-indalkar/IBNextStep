package com.infobeans.ibnextstep.placement;

import com.infobeans.ibnextstep.department.DepartmentRepository;
import com.infobeans.ibnextstep.placement.dto.AdminPlacementDashboardResponse;
import com.infobeans.ibnextstep.placement.dto.HrDashboardResponse;
import com.infobeans.ibnextstep.placement.dto.PlacementApplicationResponse;
import com.infobeans.ibnextstep.placement.dto.StudentPlacementDashboardResponse;
import com.infobeans.ibnextstep.placement.dto.UpcomingInterviewDto;
import com.infobeans.ibnextstep.user.Role;
import com.infobeans.ibnextstep.user.User;
import com.infobeans.ibnextstep.user.UserRepository;
import com.infobeans.ibnextstep.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PlacementDashboardService {

    private final CompanyRepository companyRepository;
    private final PlacementRepository placementRepository;
    private final PlacementApplicationRepository applicationRepository;
    private final PlacementApplicationService applicationService;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter YEAR_FORMAT = DateTimeFormatter.ofPattern("yyyy").withZone(ZoneId.systemDefault());

    // ---------- HR dashboard ----------

    public HrDashboardResponse getHrDashboard() {
        List<PlacementApplication> allApplications = applicationRepository.findAll();
        List<PlacementApplication> scheduled = applicationRepository.findByStatus(PlacementApplicationStatus.INTERVIEW_SCHEDULED);

        Map<String, Long> placementTrend = new HashMap<>();
        Map<String, Long> departmentWiseApplications = new HashMap<>();
        for (PlacementApplication app : allApplications) {
            if (app.getAppliedAt() != null) {
                placementTrend.merge(MONTH_FORMAT.format(app.getAppliedAt()), 1L, Long::sum);
            }
            String dept = app.getDepartmentName() != null ? app.getDepartmentName() : "Unassigned";
            departmentWiseApplications.merge(dept, 1L, Long::sum);
        }

        Map<String, Long> companyWiseHiring = new HashMap<>();
        Map<String, Long> roundWiseSelection = new HashMap<>();
        for (PlacementApplication app : allApplications) {
            if (app.getStatus() == PlacementApplicationStatus.SELECTED) {
                companyWiseHiring.merge(app.getCompanyName(), 1L, Long::sum);
            }
            for (PlacementApplication.InterviewRoundInstance round : app.getRounds()) {
                if (round.getResult() == InterviewRoundResult.QUALIFIED) {
                    roundWiseSelection.merge(round.getRoundType(), 1L, Long::sum);
                }
            }
        }

        Instant now = Instant.now();
        Instant todayStart = LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant todayEnd = todayStart.plusSeconds(86_400);

        long todaysInterviews = 0;
        long upcomingInterviews = 0;
        List<UpcomingInterviewDto> nextUpcoming = scheduled.stream()
                .flatMap(app -> app.getRounds().stream()
                        .filter(r -> r.getStatus() == InterviewRoundStatus.SCHEDULED || r.getStatus() == InterviewRoundStatus.RESCHEDULED)
                        .filter(r -> r.getScheduledAt() != null && r.getScheduledAt().isAfter(now))
                        .map(r -> UpcomingInterviewDto.builder()
                                .applicationId(app.getId())
                                .placementTitle(app.getPlacementTitle())
                                .companyName(app.getCompanyName())
                                .studentName(app.getStudentName())
                                .roundNumber(r.getRoundNumber())
                                .roundType(r.getRoundType())
                                .scheduledAt(r.getScheduledAt())
                                .venue(r.getVenue())
                                .meetingLink(r.getMeetingLink())
                                .build()))
                .sorted(Comparator.comparing(UpcomingInterviewDto::getScheduledAt))
                .toList();

        for (PlacementApplication app : scheduled) {
            for (PlacementApplication.InterviewRoundInstance round : app.getRounds()) {
                if ((round.getStatus() == InterviewRoundStatus.SCHEDULED || round.getStatus() == InterviewRoundStatus.RESCHEDULED)
                        && round.getScheduledAt() != null) {
                    if (!round.getScheduledAt().isBefore(todayStart) && round.getScheduledAt().isBefore(todayEnd)) {
                        todaysInterviews++;
                    }
                    if (round.getScheduledAt().isAfter(now)) {
                        upcomingInterviews++;
                    }
                }
            }
        }

        return HrDashboardResponse.builder()
                .totalCompanies(companyRepository.count())
                .activeCompanies(companyRepository.countByActiveTrue())
                .activePlacementDrives(placementRepository.countByStatus(PlacementStatus.PUBLISHED))
                .campusDrives(placementRepository.countByStatusAndType(PlacementStatus.PUBLISHED, PlacementType.CAMPUS))
                .offCampusDrives(placementRepository.countByStatusAndType(PlacementStatus.PUBLISHED, PlacementType.OFF_CAMPUS))
                .applicationsReceived(allApplications.size())
                .shortlisted(applicationRepository.countByStatus(PlacementApplicationStatus.SHORTLISTED))
                .interviewScheduled(scheduled.size())
                .selected(applicationRepository.countByStatus(PlacementApplicationStatus.SELECTED))
                .rejected(applicationRepository.countByStatus(PlacementApplicationStatus.REJECTED)
                        + applicationRepository.countByStatus(PlacementApplicationStatus.NOT_SELECTED))
                .todaysInterviews(todaysInterviews)
                .upcomingInterviews(upcomingInterviews)
                .placementTrend(placementTrend)
                .companyWiseHiring(companyWiseHiring)
                .departmentWiseApplications(departmentWiseApplications)
                .roundWiseSelection(roundWiseSelection)
                .nextUpcomingInterviews(nextUpcoming.stream().limit(10).toList())
                .build();
    }

    // ---------- Admin dashboard (read-only analytics) ----------

    public AdminPlacementDashboardResponse getAdminDashboard() {
        List<User> students = userRepository.findAll().stream().filter(u -> u.getRole() == Role.STUDENT).toList();
        long totalStudents = students.size();
        long activeStudents = students.stream().filter(u -> u.getStatus() == UserStatus.ACTIVE).count();

        List<PlacementApplication> selectedApplications = applicationRepository.findByStatus(PlacementApplicationStatus.SELECTED);
        Set<String> placedStudentIds = new HashSet<>();
        for (PlacementApplication app : selectedApplications) {
            placedStudentIds.add(app.getStudentId());
        }
        long placedStudents = placedStudentIds.size();
        long unplacedStudents = Math.max(0, totalStudents - placedStudents);
        double placementPercentage = totalStudents == 0 ? 0 : round(placedStudents * 100.0 / totalStudents);

        Map<String, Long> departmentWisePlacement = new HashMap<>();
        for (PlacementApplication app : selectedApplications) {
            String dept = app.getDepartmentName() != null ? app.getDepartmentName() : "Unassigned";
            departmentWisePlacement.merge(dept, 1L, Long::sum);
        }

        Map<String, Long> companyWisePlacement = new HashMap<>();
        Map<String, Long> monthlyTrend = new HashMap<>();
        Map<String, Long> yearWiseTrend = new HashMap<>();
        for (PlacementApplication app : selectedApplications) {
            companyWisePlacement.merge(app.getCompanyName(), 1L, Long::sum);
            if (app.getSelectedAt() != null) {
                monthlyTrend.merge(MONTH_FORMAT.format(app.getSelectedAt()), 1L, Long::sum);
                yearWiseTrend.merge(YEAR_FORMAT.format(app.getSelectedAt()), 1L, Long::sum);
            }
        }

        Map<String, Double> packagesByPlacement = new HashMap<>();
        for (Placement placement : placementRepository.findAll()) {
            if (placement.getPackageLpa() != null
                    && applicationRepository.countByPlacementIdAndStatus(placement.getId(), PlacementApplicationStatus.SELECTED) > 0) {
                packagesByPlacement.put(placement.getId(), placement.getPackageLpa());
            }
        }
        Double highest = packagesByPlacement.values().stream().max(Double::compareTo).orElse(null);
        Double average = packagesByPlacement.values().isEmpty() ? null
                : round(packagesByPlacement.values().stream().mapToDouble(Double::doubleValue).average().orElse(0));

        return AdminPlacementDashboardResponse.builder()
                .totalStudents(totalStudents)
                .activeStudents(activeStudents)
                .placedStudents(placedStudents)
                .unplacedStudents(unplacedStudents)
                .placementPercentage(placementPercentage)
                .totalCompanies(companyRepository.count())
                .activeCompanies(companyRepository.countByActiveTrue())
                .campusDrives(placementRepository.countByType(PlacementType.CAMPUS))
                .offCampusDrives(placementRepository.countByType(PlacementType.OFF_CAMPUS))
                .applications(applicationRepository.count())
                .selections(selectedApplications.size())
                .rejections(applicationRepository.countByStatus(PlacementApplicationStatus.REJECTED)
                        + applicationRepository.countByStatus(PlacementApplicationStatus.NOT_SELECTED))
                .highestPackageLpa(highest)
                .averagePackageLpa(average)
                .departmentWisePlacement(departmentWisePlacement)
                .companyWisePlacement(companyWisePlacement)
                .monthlyPlacementTrend(monthlyTrend)
                .yearWisePlacement(yearWiseTrend)
                .build();
    }

    // ---------- Student dashboard ----------

    public StudentPlacementDashboardResponse getStudentDashboard(String studentId) {
        List<PlacementApplication> myApplications = applicationRepository.findByStudentId(studentId);
        Instant now = Instant.now();

        List<UpcomingInterviewDto> upcoming = myApplications.stream()
                .flatMap(app -> app.getRounds().stream()
                        .filter(r -> (r.getStatus() == InterviewRoundStatus.SCHEDULED || r.getStatus() == InterviewRoundStatus.RESCHEDULED)
                                && r.getScheduledAt() != null && r.getScheduledAt().isAfter(now))
                        .map(r -> UpcomingInterviewDto.builder()
                                .applicationId(app.getId())
                                .placementTitle(app.getPlacementTitle())
                                .companyName(app.getCompanyName())
                                .studentName(app.getStudentName())
                                .roundNumber(r.getRoundNumber())
                                .roundType(r.getRoundType())
                                .scheduledAt(r.getScheduledAt())
                                .venue(r.getVenue())
                                .meetingLink(r.getMeetingLink())
                                .build()))
                .sorted(Comparator.comparing(UpcomingInterviewDto::getScheduledAt))
                .toList();

        List<StudentPlacementDashboardResponse.InterviewHistoryEntry> history = myApplications.stream()
                .flatMap(app -> app.getRounds().stream()
                        .filter(r -> r.getStatus() == InterviewRoundStatus.COMPLETED)
                        .map(r -> StudentPlacementDashboardResponse.InterviewHistoryEntry.builder()
                                .placementTitle(app.getPlacementTitle())
                                .companyName(app.getCompanyName())
                                .roundNumber(r.getRoundNumber())
                                .roundType(r.getRoundType())
                                .result(r.getResult().name())
                                .completedAt(r.getUpdatedAt())
                                .build()))
                .sorted(Comparator.comparing(StudentPlacementDashboardResponse.InterviewHistoryEntry::getCompletedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<PlacementApplicationResponse> applicationResponses = myApplications.stream()
                .map(a -> applicationService.getOne(a.getId()))
                .toList();

        long campusOpen = placementRepository.countByStatusAndType(PlacementStatus.PUBLISHED, PlacementType.CAMPUS);
        long offCampusOpen = placementRepository.countByStatusAndType(PlacementStatus.PUBLISHED, PlacementType.OFF_CAMPUS);

        return StudentPlacementDashboardResponse.builder()
                .appliedCount(myApplications.size())
                .selectedCount(myApplications.stream().filter(a -> a.getStatus() == PlacementApplicationStatus.SELECTED).count())
                .rejectedCount(myApplications.stream().filter(a -> a.getStatus() == PlacementApplicationStatus.REJECTED
                        || a.getStatus() == PlacementApplicationStatus.NOT_SELECTED).count())
                .campusOpenCount(campusOpen)
                .offCampusOpenCount(offCampusOpen)
                .upcomingInterviews(upcoming)
                .interviewHistory(history)
                .applications(applicationResponses)
                .build();
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
