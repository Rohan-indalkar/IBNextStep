package com.infobeans.ibnextstep.attendance;

import com.infobeans.ibnextstep.attendance.dto.StudentMonthlyPercentage;
import com.infobeans.ibnextstep.audit.AuditLogService;
import com.infobeans.ibnextstep.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * "System Scheduler -> Calculate Monthly Attendance -> Generate Attendance Report ->
 * Send Notification to Student -> Student Receives Monthly Attendance Summary Notification."
 *
 * Runs at 00:10 on the 1st of every month, computing the PREVIOUS month's attendance for
 * every student and notifying them — a normal summary, or a warning if below the threshold.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MonthlyAttendanceNotificationScheduler {

    private final AttendanceService attendanceService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    @Value("${attendance.warning-threshold-percentage:75}")
    private double warningThresholdPercentage;

    /** Cron: minute=10, hour=0, day-of-month=1, every month. */
    @Scheduled(cron = "0 10 0 1 * *")
    public void sendMonthlyAttendanceNotifications() {
        LocalDate previousMonth = LocalDate.now().minusMonths(1);
        int year = previousMonth.getYear();
        int month = previousMonth.getMonthValue();
        String monthName = previousMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        List<StudentMonthlyPercentage> percentages = attendanceService.getMonthlyPercentagesForAllStudents(year, month);

        int warningsSent = 0;
        for (StudentMonthlyPercentage p : percentages) {
            String title = "Monthly attendance summary — " + monthName + " " + year;
            String message;

            if (p.getAttendancePercentage() < warningThresholdPercentage) {
                message = "Warning: Your monthly attendance is below the required minimum attendance.";
                warningsSent++;
            } else {
                message = String.format("Your attendance for %s %d is %.0f%% (%d/%d days present).",
                        monthName, year, p.getAttendancePercentage(), p.getPresentCount(), p.getTotalDays());
            }

            notificationService.sendToUser(p.getStudentId(), title, message, "SYSTEM");
        }

        auditLogService.log(null, null, "SYSTEM", "MONTHLY_ATTENDANCE_NOTIFICATIONS_SENT",
                "Sent " + percentages.size() + " monthly attendance notifications for " + monthName + " " + year
                        + " (" + warningsSent + " below threshold)", null);

        log.info("Monthly attendance notifications sent for {} {}: {} students, {} warnings",
                monthName, year, percentages.size(), warningsSent);
    }
}
