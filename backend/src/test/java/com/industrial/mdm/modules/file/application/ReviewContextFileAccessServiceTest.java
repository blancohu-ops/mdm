package com.industrial.mdm.modules.file.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.common.security.UserRole;
import com.industrial.mdm.modules.enterprise.domain.EnterpriseStatus;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseEntity;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseProfileEntity;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseProfileRepository;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseRepository;
import com.industrial.mdm.modules.file.domain.FileAccessScope;
import com.industrial.mdm.modules.file.repository.StoredFileEntity;
import com.industrial.mdm.modules.iam.application.AuthorizationProfile;
import com.industrial.mdm.modules.iam.application.AuthorizationService;
import com.industrial.mdm.modules.iam.application.ReviewDomainAssignmentService;
import com.industrial.mdm.modules.iam.application.RoleAuthorizationCatalog;
import com.industrial.mdm.modules.iam.domain.context.ReviewDomainType;
import com.industrial.mdm.modules.iam.domain.permission.PermissionCode;
import com.industrial.mdm.modules.iam.domain.scope.DataScopeCode;
import com.industrial.mdm.modules.product.domain.ProductStatus;
import com.industrial.mdm.modules.product.repository.ProductEntity;
import com.industrial.mdm.modules.product.repository.ProductProfileEntity;
import com.industrial.mdm.modules.product.repository.ProductProfileRepository;
import com.industrial.mdm.modules.product.repository.ProductRepository;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ReviewContextFileAccessServiceTest {

    @Mock
    private FileService fileService;

    @Mock
    private EnterpriseRepository enterpriseRepository;

    @Mock
    private EnterpriseProfileRepository enterpriseProfileRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductProfileRepository productProfileRepository;

    @Mock
    private ReviewDomainAssignmentService reviewDomainAssignmentService;

    private ReviewContextFileAccessService reviewContextFileAccessService;

    @BeforeEach
    void setUp() {
        reviewContextFileAccessService =
                new ReviewContextFileAccessService(
                        fileService,
                        new AuthorizationService(
                                currentUser ->
                                        AuthorizationProfile.fromPolicy(
                                                new RoleAuthorizationCatalog()
                                                        .getRequired(currentUser.role()))),
                        reviewDomainAssignmentService,
                        enterpriseRepository,
                        enterpriseProfileRepository,
                        productRepository,
                        productProfileRepository,
                        new ObjectMapper());
    }

    @Test
    void reviewerCanAccessFileWhenFileBelongsToRequestedCompanyReviewContext() {
        UUID enterpriseId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        EnterpriseEntity enterprise = enterprise(enterpriseId, profileId, null);
        EnterpriseProfileEntity profile = enterpriseProfile(fileId);
        StoredFileEntity file = storedFile(fileId, enterpriseId);
        ResponseEntity<org.springframework.core.io.Resource> response =
                ResponseEntity.ok(new ByteArrayResource(new byte[0]));

        when(fileService.loadExistingFile(fileId)).thenReturn(file);
        when(enterpriseRepository.findById(enterpriseId)).thenReturn(Optional.of(enterprise));
        when(enterpriseProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(fileService.downloadStoredFile(file)).thenReturn(response);

        reviewContextFileAccessService.downloadCompanyReviewFile(
                enterpriseId, fileId, user(UserRole.REVIEWER));

        verify(reviewDomainAssignmentService)
                .assertEnterpriseAccess(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.eq(ReviewDomainType.COMPANY_REVIEW),
                        org.mockito.ArgumentMatchers.eq(enterpriseId),
                        org.mockito.ArgumentMatchers.anyString());
        verify(fileService).downloadStoredFile(file);
    }

    @Test
    void reviewerCannotAccessFileOfAnotherEnterpriseByForgedFileId() {
        UUID enterpriseId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        EnterpriseEntity enterprise = enterprise(enterpriseId, profileId, null);
        EnterpriseProfileEntity profile = enterpriseProfile(fileId);
        StoredFileEntity file = storedFile(fileId, UUID.randomUUID());

        when(fileService.loadExistingFile(fileId)).thenReturn(file);
        when(enterpriseRepository.findById(enterpriseId)).thenReturn(Optional.of(enterprise));
        when(enterpriseProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));

        assertThatThrownBy(
                        () ->
                                reviewContextFileAccessService.downloadCompanyReviewFile(
                                        enterpriseId, fileId, user(UserRole.REVIEWER)))
                .isInstanceOf(BizException.class)
                .satisfies(
                        exception ->
                                org.assertj.core.api.Assertions.assertThat(
                                                ((BizException) exception).getErrorCode())
                                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void operationsAdminCanAccessFileWhenFileBelongsToRequestedProductReviewContext() {
        UUID enterpriseId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        ProductEntity product = product(productId, enterpriseId, profileId);
        ProductProfileEntity profile = productProfile(List.of(downloadPath(fileId)));
        StoredFileEntity file = storedFile(fileId, enterpriseId);
        ResponseEntity<org.springframework.core.io.Resource> response =
                ResponseEntity.ok(new ByteArrayResource(new byte[0]));

        when(fileService.loadExistingFile(fileId)).thenReturn(file);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(fileService.downloadStoredFile(file)).thenReturn(response);

        reviewContextFileAccessService.downloadProductReviewFile(
                productId, fileId, user(UserRole.OPERATIONS_ADMIN));

        verify(reviewDomainAssignmentService)
                .assertEnterpriseAccess(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.eq(ReviewDomainType.PRODUCT_REVIEW),
                        org.mockito.ArgumentMatchers.eq(enterpriseId),
                        org.mockito.ArgumentMatchers.anyString());
        verify(fileService).downloadStoredFile(file);
    }

    @Test
    void reviewerCannotAccessFileOutsideProductReviewContextEvenIfFileExists() {
        UUID enterpriseId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID requestedFileId = UUID.randomUUID();
        ProductEntity product = product(productId, enterpriseId, profileId);
        ProductProfileEntity profile = productProfile(List.of(downloadPath(UUID.randomUUID())));
        StoredFileEntity file = storedFile(requestedFileId, enterpriseId);

        when(fileService.loadExistingFile(requestedFileId)).thenReturn(file);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productProfileRepository.findById(profileId)).thenReturn(Optional.of(profile));

        assertThatThrownBy(
                        () ->
                                reviewContextFileAccessService.downloadProductReviewFile(
                                        productId, requestedFileId, user(UserRole.REVIEWER)))
                .isInstanceOf(BizException.class)
                .satisfies(
                        exception ->
                                org.assertj.core.api.Assertions.assertThat(
                                                ((BizException) exception).getErrorCode())
                                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void reviewerCannotAccessCompanyReviewFileOutsideAssignedDomain() {
        UUID enterpriseId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();

        doThrow(new BizException(ErrorCode.FORBIDDEN, "outside assigned domain"))
                .when(reviewDomainAssignmentService)
                .assertEnterpriseAccess(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.eq(ReviewDomainType.COMPANY_REVIEW),
                        org.mockito.ArgumentMatchers.eq(enterpriseId),
                        org.mockito.ArgumentMatchers.anyString());

        assertThatThrownBy(
                        () ->
                                reviewContextFileAccessService.downloadCompanyReviewFile(
                                        enterpriseId, fileId, user(UserRole.REVIEWER)))
                .isInstanceOf(BizException.class)
                .satisfies(
                        exception ->
                                org.assertj.core.api.Assertions.assertThat(
                                                ((BizException) exception).getErrorCode())
                                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void reviewFilePermissionWithoutReviewDetailPermissionIsForbidden() {
        UUID enterpriseId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        ReviewContextFileAccessService restrictedService =
                new ReviewContextFileAccessService(
                        fileService,
                        new AuthorizationService(
                                currentUser ->
                                        new AuthorizationProfile(
                                                EnumSet.of(PermissionCode.FILE_REVIEW_CONTEXT_DOWNLOAD),
                                                EnumSet.of(DataScopeCode.ASSIGNED_DOMAIN),
                                                EnumSet.noneOf(
                                                        com.industrial.mdm.modules.iam.domain.capability.CapabilityCode.class))),
                        reviewDomainAssignmentService,
                        enterpriseRepository,
                        enterpriseProfileRepository,
                        productRepository,
                        productProfileRepository,
                        new ObjectMapper());

        assertThatThrownBy(
                        () ->
                                restrictedService.downloadCompanyReviewFile(
                                        enterpriseId, fileId, user(UserRole.REVIEWER)))
                .isInstanceOf(BizException.class)
                .satisfies(
                        exception ->
                                org.assertj.core.api.Assertions.assertThat(
                                                ((BizException) exception).getErrorCode())
                                        .isEqualTo(ErrorCode.FORBIDDEN));
    }

    private EnterpriseEntity enterprise(UUID enterpriseId, UUID workingProfileId, UUID currentProfileId) {
        EnterpriseEntity entity = new EnterpriseEntity();
        setField(EnterpriseEntity.class, entity, "id", enterpriseId);
        entity.setName("enterprise");
        entity.setStatus(EnterpriseStatus.PENDING_REVIEW);
        entity.setWorkingProfileId(workingProfileId);
        entity.setCurrentProfileId(currentProfileId);
        return entity;
    }

    private EnterpriseProfileEntity enterpriseProfile(UUID fileId) {
        EnterpriseProfileEntity entity = new EnterpriseProfileEntity();
        entity.setLogoUrl("");
        entity.setLicensePreviewUrl(downloadPath(fileId));
        return entity;
    }

    private ProductEntity product(UUID productId, UUID enterpriseId, UUID workingProfileId) {
        ProductEntity entity = new ProductEntity();
        setField(ProductEntity.class, entity, "id", productId);
        entity.setEnterpriseId(enterpriseId);
        entity.setStatus(ProductStatus.PENDING_REVIEW);
        entity.setWorkingProfileId(workingProfileId);
        return entity;
    }

    private ProductProfileEntity productProfile(List<String> attachments) {
        ProductProfileEntity entity = new ProductProfileEntity();
        entity.setAttachmentsJson(writeAttachments(attachments));
        return entity;
    }

    private StoredFileEntity storedFile(UUID fileId, UUID enterpriseId) {
        StoredFileEntity entity = new StoredFileEntity();
        setField(StoredFileEntity.class, entity, "id", fileId);
        entity.setAccessScope(FileAccessScope.PRIVATE);
        entity.setBusinessType("business-license");
        entity.setOriginalFileName("sample.pdf");
        entity.setStoredFileName("sample.pdf");
        entity.setExtension(".pdf");
        entity.setMimeType("application/pdf");
        entity.setFileSize(64L);
        entity.setStoragePath(Path.of("backend", "storage", "sample.pdf").toString());
        entity.setUploadedBy(UUID.randomUUID());
        entity.setEnterpriseId(enterpriseId);
        return entity;
    }

    private String downloadPath(UUID fileId) {
        return "/api/v1/files/" + fileId + "/download";
    }

    private String writeAttachments(List<String> attachments) {
        try {
            return new ObjectMapper().writeValueAsString(attachments);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to build attachment json", exception);
        }
    }

    private AuthenticatedUser user(UserRole role) {
        return new AuthenticatedUser(UUID.randomUUID(), role, null, "reviewer", "platform", 0);
    }

    private void setField(Class<?> type, Object target, String fieldName, Object value) {
        try {
            var field = type.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("failed to set field " + fieldName, exception);
        }
    }
}
