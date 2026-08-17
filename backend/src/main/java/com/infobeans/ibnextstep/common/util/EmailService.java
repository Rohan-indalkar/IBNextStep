package com.infobeans.ibnextstep.common.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Sends an email, but never lets a broken/unconfigured mail server break
     * the calling flow (login, user creation, etc). If sending fails —
     * typically because spring.mail.* isn't configured yet, or credentials
     * are wrong — the content is logged instead so you can still complete
     * testing (e.g. read the OTP straight from the console).
     *
     * Runs asynchronously (@Async) so composing a notification to many
     * recipients, or creating a user, doesn't block the HTTP response while
     * each individual email is sent one by one.
     */
    @Async
    public void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();

            message.setFrom("indalkarrohan466@gmail.com");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);

            log.info("Email sent successfully to {}", to);

        } catch (MailException e) {
            log.error("Mail sending failed", e);

            log.warn("=================================================================");
            log.warn("EMAIL NOT SENT");
            log.warn("To: {}", to);
            log.warn("Subject: {}", subject);
            log.warn("Body:\n{}", body);
            log.warn("=================================================================");
        }
    }

    public void sendOtp(String to, String otp, String purpose) {
        send(to, "Your OTP Code - IBNextStep",
                "Your OTP for " + purpose + " is: " + otp + "\nThis code expires in 5 minutes.\n\n" +
                        "If you did not request this, please ignore this email.");
    }

    public void sendNewAccountCredentials(String to, String tempPassword) {
        send(to, "Your IBNextStep Account Has Been Created",
                "Welcome to IBNextStep - Placement Readiness Platform.\n\n" +
                        "Login email: " + to + "\n" +
                        "Temporary password: " + tempPassword + "\n\n" +
                        "You will be required to change this password on first login.");
    }
}