package com.industrial.mdm.modules.file.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.common.security.UserRole;
import com.industrial.mdm.infrastructure.storage.StorageService;
import com.industrial.mdm.modules.file.domain.FileAccessScope;
import com.industrial.mdm.modules.iam.application.AuthorizationProfile;
import com.industrial.mdm.modules.iam.application.AuthorizationService;
import com.industrial.mdm.modules.iam.application.RoleAuthorizationCatalog;
import com.industrial.mdm.modules.file.repository.StoredFileEntity;
import com.industrial.mdm.modules.file.repository.StoredFileRepository;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class FileServiceAuthorizationTest {

    @Mock
    private StorageService storageService;

    @Mock
    private StoredFileRepository storedFileRepository;

    private FileService fileService;

    @BeforeEach
    void setUp() {
        fileService =
                new FileService(
                        storageService,
                        storedFileRepository,
                        new AuthorizationService(
                                currentUser ->
                                        AuthorizationProfile.fromPolicy(
                                                new RoleAuthorizationCatalog()
                                                        .getRequired(currentUser.role()))));
    }

    @Test
    void enterpriseOwnerCanReadPrivateFileOfOwnEnterprise() {
        UUID enterpriseId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        StoredFileEntity file = privateFile(fileId, enterpriseId);
        when(storedFileRepository.findById(fileId)).thenReturn(Optional.of(file));

        StoredFileEntity result =
                fileService.loadReadableFile(
                        fileId, user(UserRole.ENTERPRISE_OWNER, enterpriseId));

        assertThat(result).isSameAs(file);
    }

    @Test
    void enterpriseOwnerCannotReadPrivateFileOfAnotherEnterprise() {
        UUID fileId = UUID.randomUUID();
        StoredFileEntity file = privateFile(fileId, UUID.randomUUID());
        when(storedFileRepository.findById(fileId)).thenReturn(Optional.of(file));

        assertThatForbidden(
                () ->
                        fileService.loadReadableFile(
                                fileId,
                                user(UserRole.ENTERPRISE_OWNER, UUID.randomUUID())),
                "stored file does not belong to current enterprise");
    }

    @Test
    void anonymousUserCanReadPublicFileOnDownloadPath() {
        UUID fileId = UUID.randomUUID();
        StoredFileEntity file = publicFile(fileId, UUID.randomUUID());
        when(storedFileRepository.findById(fileId)).thenReturn(Optional.of(file));

        StoredFileEntity result = fileService.loadReadableFile(fileId, null);

        assertThat(result).isSameAs(file);
    }

    @Test
    void anonymousUserCannotReadPrivateFileOnDownloadPath() {
        UUID fileId = UUID.randomUUID();
        StoredFileEntity file = privateFile(fileId, UUID.randomUUID());
        when(storedFileRepository.findById(fileId)).thenReturn(Optional.of(file));

        assertThatThrownBy(() -> fileService.loadReadableFile(fileId, null))
                .isInstanceOf(BizException.class)
                .satisfies(
                        exception ->
                                assertThat(((BizException) exception).getErrorCode())
                                        .isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    void reviewerCannotReadPrivateFileWithoutExplicitGrant() {
        UUID fileId = UUID.randomUUID();
        StoredFileEntity file = privateFile(fileId, UUID.randomUUID());
        when(storedFileRepository.findById(fileId)).thenReturn(Optional.of(file));

        assertThatForbidden(
                () -> fileService.loadReadableFile(fileId, user(UserRole.REVIEWER, null)),
                "current role cannot read private files directly");
    }

    @Test
    void operationsAdminCannotReadPrivateFileWithoutExplicitGrant() {
        UUID fileId = UUID.randomUUID();
        StoredFileEntity file = privateFile(fileId, UUID.randomUUID());
        when(storedFileRepository.findById(fileId)).thenReturn(Optional.of(file));

        assertThatForbidden(
                () ->
                        fileService.loadReadableFile(
                                fileId, user(UserRole.OPERATIONS_ADMIN, null)),
                "current role cannot read private files directly");
    }

    @Test
    void enterpriseOwnerCannotUploadSensitiveFileAsPublic() {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "license.pdf",
                        "application/pdf",
                        "test".getBytes());

        assertThatThrownBy(
                        () ->
                                fileService.upload(
                                        user(UserRole.ENTERPRISE_OWNER, UUID.randomUUID()),
                                        "business-license",
                                        "public",
                                        file))
                .isInstanceOf(BizException.class)
                .satisfies(
                        exception ->
                                assertThat(((BizException) exception).getErrorCode())
                                        .isEqualTo(ErrorCode.INVALID_REQUEST));

        verifyNoInteractions(storageService);
    }

    @Test
    void reviewerCannotUploadFiles() {
        MockMultipartFile file =
                new MockMultipartFile("file", "logo.png", "image/png", "test".getBytes());

        assertThatForbidden(
                () -> fileService.upload(user(UserRole.REVIEWER, null), "enterprise-logo", "public", file),
                "only enterprise users can upload files");

        verifyNoInteractions(storageService);
    }

    private StoredFileEntity privateFile(UUID fileId, UUID enterpriseId) {
        return storedFile(fileId, enterpriseId, FileAccessScope.PRIVATE);
    }

    private StoredFileEntity publicFile(UUID fileId, UUID enterpriseId) {
        return storedFile(fileId, enterpriseId, FileAccessScope.PUBLIC);
    }

    private StoredFileEntity storedFile(UUID fileId, UUID enterpriseId, FileAccessScope scope) {
        StoredFileEntity entity = new StoredFileEntity();
        setFileId(entity, fileId);
        entity.setBusinessType("business-license");
        entity.setAccessScope(scope);
        entity.setOriginalFileName("sample.pdf");
        entity.setStoredFileName("sample.pdf");
        entity.setExtension(".pdf");
        entity.setMimeType("application/pdf");
        entity.setFileSize(128L);
        entity.setStoragePath(Path.of("backend", "storage", "sample.pdf").toString());
        entity.setUploadedBy(UUID.randomUUID());
        entity.setEnterpriseId(enterpriseId);
        return entity;
    }

    private void setFileId(StoredFileEntity entity, UUID fileId) {
        try {
            var field = StoredFileEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, fileId);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("failed to set file id", exception);
        }
    }

    private AuthenticatedUser user(UserRole role, UUID enterpriseId) {
        return new AuthenticatedUser(UUID.randomUUID(), role, enterpriseId, "tester", "org", 0);
    }

    private void assertThatForbidden(ThrowingRunnable runnable, String message) {
        assertThatThrownBy(runnable::run)
                .isInstanceOf(BizException.class)
                .satisfies(
                        exception -> {
                            BizException bizException = (BizException) exception;
                            assertThat(bizException.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
                            assertThat(bizException.getMessage()).isEqualTo(message);
                        });
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run();
    }
}
