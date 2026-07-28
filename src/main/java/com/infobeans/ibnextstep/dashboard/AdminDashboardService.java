package com.infobeans.ibnextstep.dashboard;

import com.infobeans.ibnextstep.batch.Batch;
import com.infobeans.ibnextstep.batch.BatchRepository;
import com.infobeans.ibnextstep.course.CourseRepository;
import com.infobeans.ibnextstep.placement.PlacementOpportunity;
import com.infobeans.ibnextstep.placement.PlacementOpportunityRepository;
import com.infobeans.ibnextstep.user.Role;
import com.infobeans.ibnextstep.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final BatchRepository batchRepository;
    private final CourseRepository courseRepository;
    private final PlacementOpportunityRepository opportunityRepository;

    public AdminDashboardStats getStats() {
        long students = userRepository.findByRole(Role.STUDENT, Pageable.unpaged()).getTotalElements();
        long trainers = userRepository.findByRole(Role.TRAINER, Pageable.unpaged()).getTotalElements();
        long hr = userRepository.findByRole(Role.HR, Pageable.unpaged()).getTotalElements();
        long activeBatches = batchRepository.findAll().stream()
                .filter(b -> b.getStatus() == Batch.BatchStatus.ACTIVE)
                .count();
        long courses = courseRepository.count();
        long pendingApprovals = opportunityRepository
                .findByStatus(PlacementOpportunity.OpportunityStatus.PENDING_APPROVAL, Pageable.unpaged())
                .getTotalElements();

        return new AdminDashboardStats(students, trainers, hr, activeBatches, courses, pendingApprovals);
    }
}
