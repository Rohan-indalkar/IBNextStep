package com.infobeans.ibnextstep.user.dto;

import com.infobeans.ibnextstep.user.TrainerType;
import lombok.Data;

@Data
public class UpdateUserRequest {
    private String firstName;
    private String lastName;
    private TrainerType trainerType;
    private String departmentId;
}
