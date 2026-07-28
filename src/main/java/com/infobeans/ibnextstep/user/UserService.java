package com.infobeans.ibnextstep.user;

import com.infobeans.ibnextstep.audit.AuditLogService;
import com.infobeans.ibnextstep.common.BulkImportResult;
import com.infobeans.ibnextstep.common.PagedResponse;
import com.infobeans.ibnextstep.common.exception.BadRequestException;
import com.infobeans.ibnextstep.common.exception.ResourceNotFoundException;
import com.infobeans.ibnextstep.common.util.EmailService;
import com.infobeans.ibnextstep.common.util.OtpGenerator;
import com.infobeans.ibnextstep.user.dto.CreateUserRequest;
import com.infobeans.ibnextstep.user.dto.UpdateUserRequest;
import com.infobeans.ibnextstep.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpGenerator otpGenerator;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("A user with this email already exists");
        }
        if (request.getRole() == Role.TRAINER && request.getTrainerType() == null) {
            throw new BadRequestException("Trainer type (Technical/Soft Skill) is required for trainer accounts");
        }

        String tempPassword = otpGenerator.generateTempPassword();

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(tempPassword))
                .role(request.getRole())
                .trainerType(request.getRole() == Role.TRAINER ? request.getTrainerType() : null)
                .departmentId(request.getDepartmentId())
                .status(UserStatus.ACTIVE)
                .twoFactorEnabled(true)
                .mustChangePassword(true)
                .createdAt(Instant.now())
                .build();

        user = userRepository.save(user);
        emailService.sendNewAccountCredentials(user.getEmail(), tempPassword);
        audit("USER_CREATED", "Created " + user.getRole() + " account: " + user.getEmail());

        return UserResponse.from(user);
    }

    public UserResponse updateUser(String id, UpdateUserRequest request) {
        User user = getOrThrow(id);
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getTrainerType() != null) user.setTrainerType(request.getTrainerType());
        if (request.getDepartmentId() != null) user.setDepartmentId(request.getDepartmentId());
        user = userRepository.save(user);
        audit("USER_UPDATED", "Updated user: " + user.getEmail());
        return UserResponse.from(user);
    }

    public UserResponse setStatus(String id, boolean active) {
        User user = getOrThrow(id);
        user.setStatus(active ? UserStatus.ACTIVE : UserStatus.INACTIVE);
        user = userRepository.save(user);
        audit(active ? "USER_ACTIVATED" : "USER_DEACTIVATED", "User: " + user.getEmail());
        return UserResponse.from(user);
    }

    public void resetPassword(String id) {
        User user = getOrThrow(id);
        String tempPassword = otpGenerator.generateTempPassword();
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setMustChangePassword(true);
        userRepository.save(user);
        emailService.sendNewAccountCredentials(user.getEmail(), tempPassword);
        audit("USER_PASSWORD_RESET_BY_ADMIN", "Password reset for: " + user.getEmail());
    }

    public PagedResponse<UserResponse> search(String query, Role role, Pageable pageable) {
        var page = (role != null)
                ? userRepository.findByRole(role, pageable)
                : userRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        query == null ? "" : query, query == null ? "" : query, query == null ? "" : query, pageable);
        return new PagedResponse<>(
                page.getContent().stream().map(UserResponse::from).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast()
        );
    }

    public UserResponse getById(String id) {
        return UserResponse.from(getOrThrow(id));
    }

    public User getOrThrow(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    /**
     * CSV columns expected: firstName,lastName,email,role,trainerType,departmentId
     * trainerType and departmentId may be blank depending on role.
     */
    public BulkImportResult bulkImportUsers(MultipartFile file) {
        BulkImportResult result = new BulkImportResult();
        try (var reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            CSVParser parser = CSVFormat.DEFAULT.builder()
                    .setHeader().setSkipHeaderRecord(true).setTrim(true)
                    .build()
                    .parse(reader);

            int rowNum = 1;
            for (CSVRecord record : parser) {
                rowNum++;
                result.setTotalRows(result.getTotalRows() + 1);
                try {
                    CreateUserRequest req = new CreateUserRequest();
                    req.setFirstName(record.get("firstName"));
                    req.setLastName(record.get("lastName"));
                    req.setEmail(record.get("email"));
                    req.setRole(Role.valueOf(record.get("role").trim().toUpperCase()));
                    String trainerTypeVal = parser.getHeaderNames().contains("trainerType") ? record.get("trainerType") : null;
                    if (trainerTypeVal != null && !trainerTypeVal.isBlank()) {
                        req.setTrainerType(TrainerType.valueOf(trainerTypeVal.trim().toUpperCase()));
                    }
                    String deptVal = parser.getHeaderNames().contains("departmentId") ? record.get("departmentId") : null;
                    req.setDepartmentId(deptVal);

                    createUser(req);
                    result.recordSuccess();
                } catch (Exception e) {
                    result.recordFailure(rowNum, e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new BadRequestException("Failed to read CSV file: " + e.getMessage());
        }
        audit("USER_BULK_IMPORT", "Bulk imported users: " + result.getSuccessCount() + " succeeded, "
                + result.getFailureCount() + " failed");
        return result;
    }

    private void audit(String action, String details) {
        var authEmail = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName() : "system";
        auditLogService.log(null, authEmail, "ADMIN", action, details, null);
    }
}
