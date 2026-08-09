package com.infobeans.ibnextstep.placement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * A recruiting organization HR onboards before creating placement drives
 * for it. Deactivating a company (rather than deleting it) preserves the
 * history of past drives run against it.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "companies")
public class Company {

    @Id
    private String id;

    @Indexed
    private String name;

    private String description;
    private String industry;
    private String location;
    private String websiteUrl;

    /** Relative path from FileStorageService — same pattern used elsewhere in the app. */
    private String logoPath;

    @Builder.Default
    private boolean active = true;

    private String createdByHrId;
    private String createdByHrName;

    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
}
