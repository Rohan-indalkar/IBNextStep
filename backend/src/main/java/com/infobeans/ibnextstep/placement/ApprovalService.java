package com.infobeans.ibnextstep.placement;

import com.infobeans.ibnextstep.audit.AuditLogService;
import com.infobeans.ibnextstep.common.PagedResponse;
import com.infobeans.ibnextstep.common.exception.BadRequestException;
import com.infobeans.ibnextstep.common.exception.ResourceNotFoundException;
import com.infobeans.ibnextstep.placement.dto.RejectOpportunityRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final PlacementOpportunityRepository opportunityRepository;
    private final AuditLogService auditLogService;

    public PagedResponse<PlacementOpportunity> pendingApprovals(Pageable pageable) {
        return PagedResponse.from(
                opportunityRepository.findByStatus(PlacementOpportunity.OpportunityStatus.PENDING_APPROVAL, pageable)
        );
    }

    public PlacementOpportunity approve(String id) {
        PlacementOpportunity opportunity = getOrThrow(id);
        assertPendingCampusDrive(opportunity);
        opportunity.setStatus(PlacementOpportunity.OpportunityStatus.APPROVED);
        opportunity = opportunityRepository.save(opportunity);
        audit("OPPORTUNITY_APPROVED", "Approved campus drive opportunity: " + opportunity.getTitle());
        return opportunity;
    }

    public PlacementOpportunity reject(String id, RejectOpportunityRequest request) {
        PlacementOpportunity opportunity = getOrThrow(id);
        assertPendingCampusDrive(opportunity);
        opportunity.setStatus(PlacementOpportunity.OpportunityStatus.REJECTED);
        opportunity.setRejectionReason(request.getReason());
        opportunity = opportunityRepository.save(opportunity);
        audit("OPPORTUNITY_REJECTED", "Rejected campus drive opportunity: " + opportunity.getTitle()
                + " - Reason: " + request.getReason());
        return opportunity;
    }

    private void assertPendingCampusDrive(PlacementOpportunity opportunity) {
        if (opportunity.getType() != PlacementOpportunity.OpportunityType.CAMPUS_DRIVE) {
            throw new BadRequestException("Only Campus Drive opportunities require Admin approval");
        }
        if (opportunity.getStatus() != PlacementOpportunity.OpportunityStatus.PENDING_APPROVAL) {
            throw new BadRequestException("This opportunity is not pending approval");
        }
    }

    private PlacementOpportunity getOrThrow(String id) {
        return opportunityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found: " + id));
    }

    private void audit(String action, String details) {
        var authEmail = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName() : "system";
        auditLogService.log(null, authEmail, "ADMIN", action, details, null);
    }
}
