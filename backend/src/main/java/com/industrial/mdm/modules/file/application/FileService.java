package com.industrial.mdm.modules.file.application;

import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.infrastructure.storage.StorageService;
import com.industrial.mdm.modules.file.domain.FileAccessScope;
import com.industrial.mdm.modules.file.dto.StoredFileResponse;
import com.industrial.mdm.modules.iam.application.AuthorizationService;
import com.industrial.mdm.modules.iam.domain.permission.PermissionCode;
import com.industrial.mdm.modules.file.repository.StoredFileEntity;
import com.industrial.mdm.modules.file.repository.StoredFileRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {

    private static final long MAX_FILE_BYTES = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_PUBLIC_BUSINESS_TYPES =
            Set.of(
                    "enterprise-logo",
                    "product-image",
                    "provider-logo",
                    "provider-license",
                    "service-cover",
                    "delivery-artifact");
    private static final Set<String> ALLOWED_PUBLIC_ANONYMOUS_BUSINESS_TYPES =
            Set.of("provider-logo", "provider-license");
    private static final Set<String> ENTERPRISE_UPLOAD_BUSINESS_TYPES =
            Set.of(
                    "enterprise-logo",
                    "business-license",
                    "product-image",
                    "product-attachment",
                    "import-sheet",
                    "payment-evidence");
    private static final Set<String> PROVIDER_UPLOAD_BUSINESS_TYPES =
            Set.of("provider-logo", "provider-license", "service-cover", "delivery-artifact");
    private static final Set<String> ADMIN_UPLOAD_BUSINESS_TYPES = Set.of("service-cover");
    private static final Set<String> ALLOWED_BUSINESS_TYPES =
            Set.of(
                    "enterprise-logo",
                    "business-license",
                    "product-image",
                    "product-attachment",
                    "import-sheet",
                    "provider-logo",
                    "provider-license",
                    "service-cover",
                    "payment-evidence",
                    "delivery-artifact");
    private static final Map<String, Set<String>> ALLOWED_EXTENSIONS =
            Map.of(
                    "enterprise-logo", Set.of(".png", ".jpg", ".jpeg", ".webp"),
                    "business-license", Set.of(".pdf", ".png", ".jpg", ".jpeg"),
                    "product-image", Set.of(".png", ".jpg", ".jpeg", ".webp"),
                    "product-attachment", Set.of(".pdf", ".doc", ".docx", ".xls", ".xlsx"),
                    "import-sheet", Set.of(".xlsx", ".csv"),
                    "provider-logo", Set.of(".png", ".jpg", ".jpeg", ".webp"),
                    "provider-license", Set.of(".pdf", ".png", ".jpg", ".jpeg"),
                    "service-cover", Set.of(".png", ".jpg", ".jpeg", ".webp"),
                    "payment-evidence", Set.of(".pdf", ".png", ".jpg", ".jpeg"),
                    "delivery-artifact",
                            Set.of(
                                    ".pdf",
                                    ".doc",
                                    ".docx",
                                    ".xls",
                                    ".xlsx",
                                    ".png",
                                    ".jpg",
                                    ".jpeg",
                                    ".webp"));

    private final StorageService storageService;
    private final StoredFileRepository storedFileRepository;
    private final AuthorizationService authorizationService;

    public FileService(
            StorageService storageService,
            StoredFileRepository storedFileRepository,
            AuthorizationService authorizationService) {
        this.storageService = storageService;
        this.storedFileRepository = storedFileRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public StoredFileResponse upload(
            AuthenticatedUser currentUser,
            String businessType,
            String accessScope,
            MultipartFile file) {
        return uploadInternal(currentUser, businessType, accessScope, file, false);
    }

    @Transactional
    public StoredFileResponse uploadPublic(String businessType, String accessScope, MultipartFile file) {
        return uploadInternal(null, businessType, accessScope, file, true);
    }

    private StoredFileResponse uploadInternal(
            AuthenticatedUser currentUser,
            String businessType,
            String accessScope,
            MultipartFile file,
            boolean allowAnonymous) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "file is required");
        }
        String sanitizedBusinessType = sanitizeBusinessType(businessType);
        FileAccessScope parsedScope = parseAccessScope(accessScope);
        UUID enterpriseId =
                assertUploadAllowed(currentUser, sanitizedBusinessType, parsedScope, allowAnonymous);
        String safeOriginalFilename = sanitizeOriginalFilename(file.getOriginalFilename());
        String extension = extractExtension(safeOriginalFilename);
        validateUpload(file, sanitizedBusinessType, extension);
        Path targetPath = null;
        try {
            targetPath =
                    storageService.store(
                            sanitizedBusinessType,
                            safeOriginalFilename,
                            file.getInputStream());
            StoredFileEntity entity = new StoredFileEntity();
            entity.setBusinessType(sanitizedBusinessType);
            entity.setAccessScope(parsedScope);
            entity.setOriginalFileName(safeOriginalFilename);
            entity.setStoredFileName(targetPath.getFileName().toString());
            entity.setMimeType(resolveContentType(targetPath, extension));
            entity.setExtension(extension);
            entity.setFileSize(file.getSize());
            entity.setStoragePath(targetPath.toString());
            entity.setUploadedBy(currentUser.userId());
            entity.setEnterpriseId(enterpriseId);
            entity = storedFileRepository.save(entity);
            return toResponse(entity);
        } catch (IOException exception) {
            cleanupStoredFile(targetPath);
            throw new BizException(ErrorCode.INTERNAL_ERROR, "failed to store file");
        } catch (RuntimeException exception) {
            cleanupStoredFile(targetPath);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public StoredFileResponse getMetadata(UUID fileId, AuthenticatedUser currentUser) {
        StoredFileEntity entity = loadAuthorizedFile(fileId, currentUser);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> download(UUID fileId, AuthenticatedUser currentUser) {
        StoredFileEntity entity = loadReadableFile(fileId, currentUser);
        return downloadStoredFile(entity);
    }

    @Transactional(readOnly = true)
    public StoredFileEntity loadExistingFile(UUID fileId) {
        return storedFileRepository
                .findById(fileId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "stored file not found"));
    }

    public ResponseEntity<Resource> downloadStoredFile(StoredFileEntity entity) {
        Path path = Path.of(entity.getStoragePath());
        if (!Files.exists(path)) {
            throw new BizException(ErrorCode.NOT_FOUND, "stored file not found");
        }
        String contentType = resolveContentType(path, entity.getExtension());
        Resource resource = new PathResource(path);
        ContentDisposition disposition =
                contentType.startsWith("image/")
                        ? ContentDisposition.inline().filename(entity.getOriginalFileName()).build()
                        : ContentDisposition.attachment().filename(entity.getOriginalFileName()).build();
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString())
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(entity.getFileSize())
                .body(resource);
    }

    @Transactional(readOnly = true)
    public StoredFileEntity loadAuthorizedFile(UUID fileId, AuthenticatedUser currentUser) {
        StoredFileEntity entity = loadExistingFile(fileId);
        assertReadable(entity, currentUser, true);
        return entity;
    }

    @Transactional(readOnly = true)
    public StoredFileEntity loadReadableFile(UUID fileId, AuthenticatedUser currentUser) {
        StoredFileEntity entity = loadExistingFile(fileId);
        assertReadable(entity, currentUser, false);
        return entity;
    }

    private StoredFileResponse toResponse(StoredFileEntity entity) {
        return new StoredFileResponse(
                entity.getId(),
                entity.getBusinessType(),
                entity.getAccessScope().getCode(),
                entity.getOriginalFileName(),
                entity.getMimeType(),
                entity.getExtension(),
                entity.getFileSize(),
                "/api/v1/files/" + entity.getId() + "/download",
                entity.getCreatedAt());
    }

    private FileAccessScope parseAccessScope(String accessScope) {
        if (accessScope == null || accessScope.isBlank() || "private".equalsIgnoreCase(accessScope)) {
            return FileAccessScope.PRIVATE;
        }
        if ("public".equalsIgnoreCase(accessScope)) {
            return FileAccessScope.PUBLIC;
        }
        throw new BizException(ErrorCode.INVALID_REQUEST, "unsupported file access scope");
    }

    private String sanitizeBusinessType(String businessType) {
        if (!StringUtils.hasText(businessType)) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "business type is required");
        }
        String normalized = businessType.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_BUSINESS_TYPES.contains(normalized)) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "unsupported business type");
        }
        return normalized;
    }

    private String sanitizeOriginalFilename(String originalFileName) {
        if (!StringUtils.hasText(originalFileName)) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "original file name is required");
        }
        String cleaned = org.springframework.util.StringUtils.cleanPath(originalFileName).replace("\\", "/");
        String baseName = Paths.get(cleaned).getFileName().toString();
        String sanitized = baseName.replaceAll("[\\r\\n]", "_").replaceAll("[^A-Za-z0-9._() -]", "_");
        if (sanitized.isBlank()) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "original file name is invalid");
        }
        return sanitized;
    }

    private String extractExtension(String originalFileName) {
        if (originalFileName == null || !originalFileName.contains(".")) {
            return "";
        }
        return originalFileName.substring(originalFileName.lastIndexOf('.')).toLowerCase(Locale.ROOT);
    }

    private void validateUpload(MultipartFile file, String businessType, String extension) {
        if (file.getSize() <= 0) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "file is empty");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "file size exceeds 10MB limit");
        }
        if (extension.isBlank()) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "file extension is required");
        }
        Set<String> allowedExtensions = ALLOWED_EXTENSIONS.getOrDefault(businessType, Set.of());
        if (!allowedExtensions.contains(extension)) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "file extension is not allowed for business type");
        }
    }

    private String resolveContentType(Path path, String extension) {
        try {
            String detected = Files.probeContentType(path);
            if (detected != null && !detected.isBlank()) {
                return detected;
            }
        } catch (IOException ignored) {
            // Fall back to extension mapping below.
        }
        return switch (extension) {
            case ".png" -> MediaType.IMAGE_PNG_VALUE;
            case ".jpg", ".jpeg" -> MediaType.IMAGE_JPEG_VALUE;
            case ".webp" -> "image/webp";
            case ".pdf" -> MediaType.APPLICATION_PDF_VALUE;
            case ".csv" -> "text/csv";
            case ".xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case ".xls" -> "application/vnd.ms-excel";
            case ".doc" -> "application/msword";
            case ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> MediaType.APPLICATION_OCTET_STREAM_VALUE;
        };
    }

    private void cleanupStoredFile(Path targetPath) {
        if (targetPath == null) {
            return;
        }
        try {
            Files.deleteIfExists(targetPath);
        } catch (IOException ignored) {
            // Cleanup failure should not override the original exception.
        }
    }

    private void assertAuthenticated(AuthenticatedUser currentUser) {
        authorizationService.requireAuthenticated(currentUser, "authentication is required");
    }

    private UUID assertUploadAllowed(
            AuthenticatedUser currentUser,
            String businessType,
            FileAccessScope accessScope,
            boolean allowAnonymous) {
        if (currentUser == null) {
            if (allowAnonymous
                    && accessScope == FileAccessScope.PUBLIC
                    && ALLOWED_PUBLIC_ANONYMOUS_BUSINESS_TYPES.contains(businessType)) {
                return null;
            }
            throw new BizException(ErrorCode.UNAUTHORIZED, "authentication is required");
        }

        String uploadDeniedMessage =
                currentUser.enterpriseId() == null && currentUser.serviceProviderId() == null
                        ? "only enterprise users can upload files"
                        : "current account cannot upload files";
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.FILE_ASSET_UPLOAD,
                uploadDeniedMessage);
        if (accessScope == FileAccessScope.PUBLIC
                && !ALLOWED_PUBLIC_BUSINESS_TYPES.contains(businessType)) {
            throw new BizException(
                    ErrorCode.INVALID_REQUEST,
                    "public access is not allowed for the current business type");
        }

        if (currentUser.enterpriseId() != null) {
            if (!ENTERPRISE_UPLOAD_BUSINESS_TYPES.contains(businessType)) {
                throw new BizException(
                        ErrorCode.FORBIDDEN, "current enterprise account cannot upload this file type");
            }
            return currentUser.enterpriseId();
        }

        if (currentUser.serviceProviderId() != null) {
            if (!PROVIDER_UPLOAD_BUSINESS_TYPES.contains(businessType)) {
                throw new BizException(
                        ErrorCode.FORBIDDEN, "current provider account cannot upload this file type");
            }
            return null;
        }

        if (!ADMIN_UPLOAD_BUSINESS_TYPES.contains(businessType)) {
            throw new BizException(
                    ErrorCode.FORBIDDEN, "current platform account cannot upload this file type");
        }
        return null;
    }

    private void assertReadable(
            StoredFileEntity entity, AuthenticatedUser currentUser, boolean requireAuthentication) {
        if (entity.getAccessScope() == FileAccessScope.PUBLIC && !requireAuthentication) {
            return;
        }

        assertAuthenticated(currentUser);
        if (entity.getAccessScope() == FileAccessScope.PUBLIC) {
            return;
        }

        PermissionCode permission =
                requireAuthentication
                        ? PermissionCode.FILE_ASSET_READ
                        : PermissionCode.FILE_ASSET_DOWNLOAD;
        if (!authorizationService.hasPermission(currentUser, permission)) {
            throw new BizException(
                    ErrorCode.FORBIDDEN, "current role cannot read private files directly");
        }

        if (currentUser.enterpriseId() == null && currentUser.serviceProviderId() == null) {
            return;
        }

        if (currentUser.enterpriseId() != null) {
            authorizationService.assertEnterprisePermission(
                    currentUser,
                    permission,
                    entity.getEnterpriseId(),
                    "stored file does not belong to current enterprise");
            return;
        }

        if (entity.getUploadedBy() != null && entity.getUploadedBy().equals(currentUser.userId())) {
            return;
        }
        throw new BizException(ErrorCode.FORBIDDEN, "stored file does not belong to current provider");
    }
}
