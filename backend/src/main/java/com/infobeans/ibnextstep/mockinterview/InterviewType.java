package com.infobeans.ibnextstep.mockinterview;

import com.infobeans.ibnextstep.user.TrainerType;

public enum InterviewType {
    TECHNICAL(TrainerType.TECHNICAL),
    HR(TrainerType.SOFT_SKILL),
    SOFT_SKILLS(TrainerType.SOFT_SKILL);

    /** Which trainer type is allowed to conduct this kind of interview. */
    private final TrainerType requiredTrainerType;

    InterviewType(TrainerType requiredTrainerType) {
        this.requiredTrainerType = requiredTrainerType;
    }

    public TrainerType getRequiredTrainerType() {
        return requiredTrainerType;
    }
}
