package com.infobeans.ibnextstep.user.dto;

import com.infobeans.ibnextstep.user.Role;
import com.infobeans.ibnextstep.user.TrainerType;
import com.infobeans.ibnextstep.user.User;
import com.infobeans.ibnextstep.user.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private TrainerType trainerType;
    private UserStatus status;
    private String departmentId;
    private boolean twoFactorEnabled;
    private Instant createdAt;

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.getTrainerType(),
                user.getStatus(),
                user.getDepartmentId(),
                user.isTwoFactorEnabled(),
                user.getCreatedAt()
        );
    }
}
