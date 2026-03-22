package com.industrial.mdm.infrastructure.storage;

import com.industrial.mdm.config.StorageProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Profile({"default", "dev"})
public class LocalFileStorageService implements StorageService {

    private final StorageProperties storageProperties;

    public LocalFileStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public Path store(String businessType, String originalFilename, InputStream inputStream)
            throws IOException {
        String sanitizedBusinessType = sanitizePathSegment(businessType, "misc");
        String extension = extractExtension(originalFilename);
        Path storageRoot = Path.of(storageProperties.localRoot()).toAbsolutePath().normalize();
        Path targetDirectory =
                storageRoot
                        .resolve(sanitizedBusinessType)
                        .resolve(LocalDate.now().toString());
        targetDirectory = targetDirectory.normalize();
        if (!targetDirectory.startsWith(storageRoot)) {
            throw new IOException("resolved target directory escaped storage root");
        }
        Files.createDirectories(targetDirectory);
        Path targetPath = targetDirectory.resolve(java.util.UUID.randomUUID() + extension).normalize();
        if (!targetPath.startsWith(storageRoot)) {
            throw new IOException("resolved target file escaped storage root");
        }
        Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        return targetPath;
    }

    private String extractExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "";
        }
        String cleanName = Path.of(originalFilename).getFileName().toString();
        int lastDotIndex = cleanName.lastIndexOf('.');
        if (lastDotIndex < 0 || lastDotIndex == cleanName.length() - 1) {
            return "";
        }
        String rawExtension = cleanName.substring(lastDotIndex + 1).toLowerCase(Locale.ROOT);
        if (!rawExtension.matches("[a-z0-9]{1,10}")) {
            return "";
        }
        return "." + rawExtension;
    }

    private String sanitizePathSegment(String value, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        String sanitized = value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-");
        sanitized = sanitized.replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
        return sanitized.isBlank() ? fallback : sanitized;
    }
}
