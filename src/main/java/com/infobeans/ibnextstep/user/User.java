package com.infobeans.ibnextstep.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String firstName;
    private String lastName;

    @Indexed(unique = true)
    private String email;

    /** BCrypt-hashed. Never returned in any response DTO. */
    private String password;

    private Role role;

    /** Only set when role == TRAINER. */
    private TrainerType trainerType;

    private UserStatus status;

    // --- Department linkage (Admin's Org Settings) ---
    private String departmentId;

    // --- 2FA (email OTP) ---
    private boolean twoFactorEnabled;

    // --- Password reset / OTP tracking ---
    @Field("otp_code")
    private String otpCode;
    @Field("otp_expiry")
    private Instant otpExpiry;
    @Field("otp_purpose")
    private OtpPurpose otpPurpose;

    private boolean mustChangePassword;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum OtpPurpose {
        LOGIN_2FA,
        PASSWORD_RESET
    }
}
