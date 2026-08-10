package com.infobeans.ibnextstep.batch;

import com.infobeans.ibnextstep.batch.dto.TrainerBatchSummaryResponse;
import com.infobeans.ibnextstep.batch.dto.TrainerDashboardStats;
import com.infobeans.ibnextstep.common.exception.BadRequestException;
import com.infobeans.ibnextstep.common.exception.ResourceNotFoundException;
import com.infobeans.ibnextstep.user.Role;
import com.infobeans.ibnextstep.user.User;
import com.infobeans.ibnextstep.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Trainer-scoped read access to batches. BatchController/BatchService stay
 * ADMIN-only (hasRole('ADMIN')) — this is the separate, narrower surface a
 * logged-in trainer is allowed to hit for their own "My batches" overview,
 * mirroring the trainerId lookup pattern already used internally by
 * ResumeService / TrainerQuizService / TrainerAssessmentService.
 */
@Service
@RequiredArgsConstructor
public class TrainerBatchService {

    private final BatchRepository batchRepository;
    private final UserRepository userRepository;

    /** All batches where this trainer is assigned as technical and/or soft-skill trainer. */
    public List<TrainerBatchSummaryResponse> myBatches(String trainerEmail) {
        User trainer = getTrainer(trainerEmail);
        return batchRepository
                .findByTechnicalTrainerIdOrSoftSkillTrainerId(trainer.getId(), trainer.getId())
                .stream()
                .map(batch -> TrainerBatchSummaryResponse.from(batch, trainer.getId()))
                .toList();
    }

    /** Powers the three stat cards on the trainer Overview screen. */
    public TrainerDashboardStats stats(String trainerEmail) {
        User trainer = getTrainer(trainerEmail);
        List<Batch> batches = batchRepository
                .findByTechnicalTrainerIdOrSoftSkillTrainerId(trainer.getId(), trainer.getId());

        long activeBatches = batches.stream()
                .filter(b -> b.getStatus() == Batch.BatchStatus.ACTIVE)
                .count();

        // De-duplicated across batches, since bulk-assigned students could in theory
        // appear in more than one of this trainer's batches.
        Set<String> distinctStudentIds = new HashSet<>();
        for (Batch batch : batches) {
            if (batch.getStudentIds() != null) {
                distinctStudentIds.addAll(batch.getStudentIds());
            }
        }

        return TrainerDashboardStats.builder()
                .myBatches(batches.size())
                .activeBatches(activeBatches)
                .totalStudents(distinctStudentIds.size())
                .build();
    }

    private User getTrainer(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer account not found"));
        if (user.getRole() != Role.TRAINER) {
            throw new BadRequestException("Only trainers can perform this action");
        }
        return user;
    }
}
