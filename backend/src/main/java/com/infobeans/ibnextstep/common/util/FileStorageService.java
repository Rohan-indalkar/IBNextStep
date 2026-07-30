package com.infobeans.ibnextstep.common.util;

import com.infobeans.ibnextstep.common.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Minimal local-disk storage for uploaded files.
 *
 * No cloud storage SDK (S3 etc.) is wired into this project yet, so files are
 * written under app.storage.upload-dir, namespaced by sub-folder (e.g.
 * "study-materials"), and referenced by a random file id + the relative path
 * we hand back. Swap this out for an S3-backed implementation later without
 * touching callers — they only depend on store()/load()/delete().
 */
@Service
@Slf4j
public class FileStorageService {

    private final Path rootLocation;

    public FileStorageService(@Value("${app.storage.upload-dir:./uploads}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize storage directory: " + rootLocation, e);
        }
    }

    /**
     * Saves the file under {subFolder}/{uuid}-{sanitizedOriginalName} and returns
     * the path relative to the storage root (what gets persisted on the entity).
     */
    public String store(MultipartFile file, String subFolder) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Cannot upload an empty file");
        }

        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String sanitized = original.replaceAll("[^a-zA-Z0-9._-]", "_");
        String relativePath = subFolder + "/" + UUID.randomUUID() + "-" + sanitized;

        try {
            Path destination = rootLocation.resolve(relativePath).normalize();
            if (!destination.getParent().startsWith(rootLocation)) {
                throw new BadRequestException("Invalid file path");
            }
            Files.createDirectories(destination.getParent());
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return relativePath;
        } catch (IOException e) {
            log.error("Failed to store file {}", original, e);
            throw new BadRequestException("Failed to store file: " + original);
        }
    }

    public Resource loadAsResource(String relativePath) {
        try {
            Path file = rootLocation.resolve(relativePath).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BadRequestException("File not found on disk: " + relativePath);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new BadRequestException("Invalid file reference: " + relativePath);
        }
    }

    public void delete(String relativePath) {
        if (relativePath == null) return;
        try {
            Files.deleteIfExists(rootLocation.resolve(relativePath).normalize());
        } catch (IOException e) {
            log.warn("Failed to delete file {} (continuing)", relativePath, e);
        }
    }
}
