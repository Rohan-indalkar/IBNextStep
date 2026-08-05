package com.infobeans.ibnextstep.studentevaluation;

import com.infobeans.ibnextstep.common.ApiResponse;
import com.infobeans.ibnextstep.studentevaluation.dto.EvaluationRubricConfigResponse;
import com.infobeans.ibnextstep.studentevaluation.dto.UpdateEvaluationRubricRequest;
import com.infobeans.ibnextstep.user.TrainerType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin control over the rubric skill list scored for each trainer type.
 * Previously fixed in code; now configurable, with a built-in default used
 * whenever an admin hasn't saved a custom list. Editing a rubric here only
 * affects evaluations submitted from now on — already-submitted records
 * keep the skill names they were originally scored against.
 */
@RestController
@RequestMapping("/api/admin/evaluation-rubrics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminEvaluationRubricController {

    private final StudentEvaluationService studentEvaluationService;

    @GetMapping
    public ApiResponse<List<EvaluationRubricConfigResponse>> getAll() {
        return ApiResponse.success(studentEvaluationService.getAllRubricConfigs());
    }

    @GetMapping("/{trainerType}")
    public ApiResponse<EvaluationRubricConfigResponse> getOne(@PathVariable TrainerType trainerType) {
        return ApiResponse.success(studentEvaluationService.getRubricConfig(trainerType));
    }

    @PutMapping("/{trainerType}")
    public ApiResponse<EvaluationRubricConfigResponse> update(
            Authentication authentication,
            @PathVariable TrainerType trainerType,
            @Valid @RequestBody UpdateEvaluationRubricRequest request) {
        return ApiResponse.success(
                "Rubric updated",
                studentEvaluationService.updateRubricConfig(authentication.getName(), trainerType, request)
        );
    }
}
