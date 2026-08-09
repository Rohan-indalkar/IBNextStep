package com.infobeans.ibnextstep.user.dto;

import com.infobeans.ibnextstep.user.Role;
import com.infobeans.ibnextstep.user.TrainerType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateUserRequest {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    @Email
    private String email;

    @NotNull
    private Role role;

    /** Required only when role == TRAINER. Validated in service layer. */
    private TrainerType trainerType;

    private String departmentId;
}
