package com.infobeans.ibnextstep.batch;

import com.infobeans.ibnextstep.batch.dto.AssignStudentsRequest;
import com.infobeans.ibnextstep.batch.dto.AssignTrainerRequest;
import com.infobeans.ibnextstep.batch.dto.BatchRequest;
import com.infobeans.ibnextstep.common.ApiResponse;
import com.infobeans.ibnextstep.common.BulkImportResult;
import com.infobeans.ibnextstep.common.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/batches")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BatchController {

    private final BatchService batchService;

    @PostMapping
    public ApiResponse<Batch> create(@Valid @RequestBody BatchRequest request) {
        return ApiResponse.success("Batch created", batchService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Batch> update(@PathVariable String id, @Valid @RequestBody BatchRequest request) {
        return ApiResponse.success("Batch updated", batchService.update(id, request));
    }

    @PutMapping("/{id}/technical-trainer")
    public ApiResponse<Batch> assignTechnicalTrainer(@PathVariable String id, @Valid @RequestBody AssignTrainerRequest request) {
        return ApiResponse.success("Technical trainer assigned", batchService.assignTechnicalTrainer(id, request));
    }

    @PutMapping("/{id}/soft-skill-trainer")
    public ApiResponse<Batch> assignSoftSkillTrainer(@PathVariable String id, @Valid @RequestBody AssignTrainerRequest request) {
        return ApiResponse.success("Soft skill trainer assigned", batchService.assignSoftSkillTrainer(id, request));
    }

    @PutMapping("/{id}/technical-trainer/change")
    public ApiResponse<Batch> changeTechnicalTrainer(@PathVariable String id, @Valid @RequestBody AssignTrainerRequest request) {
        return ApiResponse.success("Technical trainer changed", batchService.changeTrainer(id, com.infobeans.ibnextstep.user.TrainerType.TECHNICAL, request));
    }

    @PutMapping("/{id}/soft-skill-trainer/change")
    public ApiResponse<Batch> changeSoftSkillTrainer(@PathVariable String id, @Valid @RequestBody AssignTrainerRequest request) {
        return ApiResponse.success("Soft skill trainer changed", batchService.changeTrainer(id, com.infobeans.ibnextstep.user.TrainerType.SOFT_SKILL, request));
    }

    @GetMapping("/{id}/timetable")
    public ApiResponse<java.util.List<Batch.TimetableEntry>> getTimetable(@PathVariable String id) {
        return ApiResponse.success(batchService.getTimetable(id).getTimetable());
    }

    @PostMapping("/{id}/timetable")
    public ApiResponse<Batch> addTimetableEntry(@PathVariable String id, @Valid @RequestBody com.infobeans.ibnextstep.batch.dto.TimetableEntryRequest request) {
        return ApiResponse.success("Timetable entry added", batchService.addTimetableEntry(id, request));
    }

    @PutMapping("/{id}/students")
    public ApiResponse<Batch> assignStudents(@PathVariable String id, @Valid @RequestBody AssignStudentsRequest request) {
        return ApiResponse.success("Students assigned", batchService.assignStudents(id, request));
    }

    @PostMapping("/{id}/students/bulk-import")
    public ApiResponse<BulkImportResult> bulkImportStudents(@PathVariable String id, @RequestParam("file") MultipartFile file) {
        return ApiResponse.success("Bulk student import processed", batchService.bulkImportStudents(id, file));
    }

    @DeleteMapping("/{id}/students/{studentId}")
    public ApiResponse<Batch> removeStudent(@PathVariable String id, @PathVariable String studentId) {
        return ApiResponse.success("Student removed from batch", batchService.removeStudent(id, studentId));
    }

    @PatchMapping("/{id}/deactivate")
    public ApiResponse<Batch> deactivate(@PathVariable String id) {
        return ApiResponse.success("Batch deactivated", batchService.deactivate(id));
    }

    @GetMapping
    public ApiResponse<PagedResponse<Batch>> search(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(batchService.search(name, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<Batch> getById(@PathVariable String id) {
        return ApiResponse.success(batchService.getOrThrow(id));
    }
}
