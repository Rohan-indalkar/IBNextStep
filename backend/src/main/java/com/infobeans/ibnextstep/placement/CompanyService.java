package com.infobeans.ibnextstep.placement;

import com.infobeans.ibnextstep.audit.AuditLogService;
import com.infobeans.ibnextstep.common.PagedResponse;
import com.infobeans.ibnextstep.common.exception.BadRequestException;
import com.infobeans.ibnextstep.common.exception.ResourceNotFoundException;
import com.infobeans.ibnextstep.common.util.FileStorageService;
import com.infobeans.ibnextstep.placement.dto.CompanyResponse;
import com.infobeans.ibnextstep.placement.dto.CreateCompanyRequest;
import com.infobeans.ibnextstep.placement.dto.UpdateCompanyRequest;
import com.infobeans.ibnextstep.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final FileStorageService fileStorageService;
    private final AuditLogService auditLogService;

    public CompanyResponse create(User hr, CreateCompanyRequest request) {
        if (companyRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BadRequestException("A company named '" + request.getName() + "' already exists");
        }

        Company company = Company.builder()
                .name(request.getName())
                .description(request.getDescription())
                .industry(request.getIndustry())
                .location(request.getLocation())
                .websiteUrl(request.getWebsiteUrl())
                .active(true)
                .createdByHrId(hr.getId())
                .createdByHrName(fullName(hr))
                .build();
        company = companyRepository.save(company);

        auditLogService.log(hr.getId(), hr.getEmail(), hr.getRole().name(), "COMPANY_CREATED",
                "Created company " + company.getName(), null);

        return toResponse(company);
    }

    public CompanyResponse update(User hr, String companyId, UpdateCompanyRequest request) {
        Company company = getCompanyOrThrow(companyId);

        company.setName(request.getName());
        company.setDescription(request.getDescription());
        company.setIndustry(request.getIndustry());
        company.setLocation(request.getLocation());
        company.setWebsiteUrl(request.getWebsiteUrl());
        company = companyRepository.save(company);

        auditLogService.log(hr.getId(), hr.getEmail(), hr.getRole().name(), "COMPANY_UPDATED",
                "Updated company " + company.getName(), null);

        return toResponse(company);
    }

    public CompanyResponse uploadLogo(User hr, String companyId, MultipartFile file) {
        Company company = getCompanyOrThrow(companyId);
        String oldLogoPath = company.getLogoPath();

        String path = fileStorageService.store(file, "company-logos");
        company.setLogoPath(path);
        company = companyRepository.save(company);

        if (oldLogoPath != null) {
            fileStorageService.delete(oldLogoPath);
        }

        auditLogService.log(hr.getId(), hr.getEmail(), hr.getRole().name(), "COMPANY_LOGO_UPDATED",
                "Updated logo for company " + company.getName(), null);

        return toResponse(company);
    }

    public CompanyResponse setActive(User hr, String companyId, boolean active) {
        Company company = getCompanyOrThrow(companyId);
        company.setActive(active);
        company = companyRepository.save(company);

        auditLogService.log(hr.getId(), hr.getEmail(), hr.getRole().name(),
                active ? "COMPANY_ACTIVATED" : "COMPANY_DEACTIVATED",
                (active ? "Activated" : "Deactivated") + " company " + company.getName(), null);

        return toResponse(company);
    }

    public PagedResponse<CompanyResponse> search(String query, Boolean active, Pageable pageable) {
        Page<Company> page = query != null && !query.isBlank()
                ? companyRepository.findByNameContainingIgnoreCase(query, pageable)
                : active != null
                        ? companyRepository.findByActive(active, pageable)
                        : companyRepository.findAll(pageable);
        return PagedResponse.from(page.map(this::toResponse));
    }

    public CompanyResponse getById(String companyId) {
        return toResponse(getCompanyOrThrow(companyId));
    }

    Company getCompanyOrThrow(String companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + companyId));
    }

    private String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }

    private CompanyResponse toResponse(Company c) {
        return CompanyResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .industry(c.getIndustry())
                .location(c.getLocation())
                .websiteUrl(c.getWebsiteUrl())
                .logoPath(c.getLogoPath())
                .active(c.isActive())
                .createdByHrName(c.getCreatedByHrName())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
