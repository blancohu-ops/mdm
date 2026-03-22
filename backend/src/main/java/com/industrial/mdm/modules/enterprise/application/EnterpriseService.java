package com.industrial.mdm.modules.enterprise.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.common.security.UserRole;
import com.industrial.mdm.modules.auth.repository.UserRepository;
import com.industrial.mdm.modules.enterprise.domain.EnterpriseStatus;
import com.industrial.mdm.modules.enterprise.dto.CompanyProfileResponse;
import com.industrial.mdm.modules.enterprise.dto.EnterpriseLatestSubmissionResponse;
import com.industrial.mdm.modules.enterprise.dto.EnterpriseProfileResponse;
import com.industrial.mdm.modules.enterprise.dto.EnterpriseProfileSaveRequest;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseEntity;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseProfileEntity;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseProfileRepository;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseRepository;
import com.industrial.mdm.modules.enterpriseReview.domain.EnterpriseSubmissionStatus;
import com.industrial.mdm.modules.enterpriseReview.domain.EnterpriseSubmissionType;
import com.industrial.mdm.modules.enterpriseReview.repository.EnterpriseSubmissionRecordEntity;
import com.industrial.mdm.modules.enterpriseReview.repository.EnterpriseSubmissionRecordRepository;
import com.industrial.mdm.modules.enterpriseReview.repository.EnterpriseSubmissionSnapshotEntity;
import com.industrial.mdm.modules.enterpriseReview.repository.EnterpriseSubmissionSnapshotRepository;
import com.industrial.mdm.modules.product.repository.ProductRepository;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnterpriseService {

    private final EnterpriseRepository enterpriseRepository;
    private final EnterpriseProfileRepository enterpriseProfileRepository;
    private final EnterpriseSubmissionRecordRepository enterpriseSubmissionRecordRepository;
    private final EnterpriseSubmissionSnapshotRepository enterpriseSubmissionSnapshotRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    public EnterpriseService(
            EnterpriseRepository enterpriseRepository,
            EnterpriseProfileRepository enterpriseProfileRepository,
            EnterpriseSubmissionRecordRepository enterpriseSubmissionRecordRepository,
            EnterpriseSubmissionSnapshotRepository enterpriseSubmissionSnapshotRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            ObjectMapper objectMapper) {
        this.enterpriseRepository = enterpriseRepository;
        this.enterpriseProfileRepository = enterpriseProfileRepository;
        this.enterpriseSubmissionRecordRepository = enterpriseSubmissionRecordRepository;
        this.enterpriseSubmissionSnapshotRepository = enterpriseSubmissionSnapshotRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public EnterpriseProfileResponse getCurrentProfile(AuthenticatedUser currentUser) {
        EnterpriseEntity enterprise = findEnterpriseOfCurrentUser(currentUser);
        EnterpriseProfileEntity profile = resolveProfileForView(enterprise);
        return new EnterpriseProfileResponse(toCompanyProfileResponse(enterprise, profile));
    }

    @Transactional
    public EnterpriseProfileResponse saveProfile(
            AuthenticatedUser currentUser, EnterpriseProfileSaveRequest request) {
        EnterpriseEntity enterprise = findEnterpriseOfCurrentUser(currentUser);
        if (!enterprise.getStatus().canEdit() || enterprise.getStatus() == EnterpriseStatus.FROZEN) {
            throw new BizException(ErrorCode.FORBIDDEN, "当前状态不允许编辑企业资料");
        }

        EnterpriseProfileEntity workingProfile = resolveWorkingProfileForEdit(enterprise);
        applyProfileRequest(workingProfile, request);
        workingProfile = enterpriseProfileRepository.save(workingProfile);
        enterprise.setName(request.name());
        enterprise.setWorkingProfileId(workingProfile.getId());
        enterpriseRepository.save(enterprise);

        userRepository.findByEnterpriseId(enterprise.getId()).forEach(
                user -> {
                    user.setDisplayName(request.contactName());
                    user.setOrganization(request.name());
                });
        return new EnterpriseProfileResponse(toCompanyProfileResponse(enterprise, workingProfile));
    }

    @Transactional
    public EnterpriseLatestSubmissionResponse submitForReview(AuthenticatedUser currentUser) {
        EnterpriseEntity enterprise = findEnterpriseOfCurrentUser(currentUser);
        if (!enterprise.getStatus().canSubmit() || enterprise.getStatus() == EnterpriseStatus.FROZEN) {
            throw new BizException(ErrorCode.FORBIDDEN, "当前状态不允许提交审核");
        }
        if (enterprise.getStatus() == EnterpriseStatus.PENDING_REVIEW) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "企业资料正在审核中");
        }
        EnterpriseProfileEntity workingProfile = loadRequiredWorkingProfile(enterprise);
        validateProfileCompleteness(workingProfile);

        EnterpriseSubmissionRecordEntity submission = new EnterpriseSubmissionRecordEntity();
        submission.setEnterpriseId(enterprise.getId());
        submission.setSubmissionType(
                enterprise.getCurrentProfileId() == null
                        ? EnterpriseSubmissionType.ONBOARDING
                        : EnterpriseSubmissionType.CHANGE);
        submission.setStatus(EnterpriseSubmissionStatus.PENDING_REVIEW);
        submission.setSubmissionName(workingProfile.getName());
        submission.setSubmissionSocialCreditCode(workingProfile.getSocialCreditCode());
        submission.setSubmissionIndustry(workingProfile.getIndustry());
        submission.setSubmissionContactName(workingProfile.getContactName());
        submission.setSubmissionContactPhone(workingProfile.getContactPhone());
        submission.setSubmittedBy(currentUser.userId());
        submission.setSubmittedAt(OffsetDateTime.now());
        submission = enterpriseSubmissionRecordRepository.save(submission);

        EnterpriseSubmissionSnapshotEntity snapshot = new EnterpriseSubmissionSnapshotEntity();
        snapshot.setEnterpriseId(enterprise.getId());
        snapshot.setSubmissionId(submission.getId());
        snapshot.setPayloadJson(writeSnapshot(enterprise, workingProfile));
        snapshot = enterpriseSubmissionSnapshotRepository.save(snapshot);

        submission.setSnapshotId(snapshot.getId());
        enterpriseSubmissionRecordRepository.save(submission);

        enterprise.setStatus(EnterpriseStatus.PENDING_REVIEW);
        enterprise.setLatestSubmissionAt(submission.getSubmittedAt());
        enterprise.setLastReviewComment(null);
        enterpriseRepository.save(enterprise);

        return new EnterpriseLatestSubmissionResponse(
                submission.getId(),
                submission.getSubmissionType().getCode(),
                submission.getStatus().getCode(),
                submission.getSubmittedAt(),
                submission.getReviewComment());
    }

    @Transactional(readOnly = true)
    public EnterpriseLatestSubmissionResponse getLatestSubmission(AuthenticatedUser currentUser) {
        EnterpriseEntity enterprise = findEnterpriseOfCurrentUser(currentUser);
        return enterpriseSubmissionRecordRepository
                .findTopByEnterpriseIdOrderBySubmittedAtDesc(enterprise.getId())
                .map(
                        record ->
                                new EnterpriseLatestSubmissionResponse(
                                        record.getId(),
                                        record.getSubmissionType().getCode(),
                                        record.getStatus().getCode(),
                                        record.getSubmittedAt(),
                                        record.getReviewComment()))
                .orElse(null);
    }

    private EnterpriseEntity findEnterpriseOfCurrentUser(AuthenticatedUser currentUser) {
        if (currentUser == null
                || currentUser.role() != UserRole.ENTERPRISE_OWNER
                || currentUser.enterpriseId() == null) {
            throw new BizException(ErrorCode.FORBIDDEN, "当前账号不属于企业主账号");
        }
        return enterpriseRepository
                .findById(currentUser.enterpriseId())
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "企业不存在"));
    }

    private EnterpriseProfileEntity resolveProfileForView(EnterpriseEntity enterprise) {
        UUID profileId =
                enterprise.getWorkingProfileId() != null
                        ? enterprise.getWorkingProfileId()
                        : enterprise.getCurrentProfileId();
        if (profileId == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "企业资料不存在");
        }
        return enterpriseProfileRepository
                .findById(profileId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "企业资料不存在"));
    }

    private EnterpriseProfileEntity resolveWorkingProfileForEdit(EnterpriseEntity enterprise) {
        if (enterprise.getWorkingProfileId() == null) {
            EnterpriseProfileEntity profile = new EnterpriseProfileEntity();
            profile.setEnterpriseId(enterprise.getId());
            profile.setVersionNo(nextVersion(enterprise.getId()));
            return profile;
        }
        EnterpriseProfileEntity currentWorking =
                enterpriseProfileRepository
                        .findById(enterprise.getWorkingProfileId())
                        .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "企业资料不存在"));
        if (enterprise.getStatus() == EnterpriseStatus.APPROVED
                && enterprise.getCurrentProfileId() != null
                && enterprise.getCurrentProfileId().equals(enterprise.getWorkingProfileId())) {
            EnterpriseProfileEntity next = currentWorking.copyAsNewVersion();
            next = enterpriseProfileRepository.save(next);
            enterprise.setWorkingProfileId(next.getId());
            enterpriseRepository.save(enterprise);
            return next;
        }
        return currentWorking;
    }

    private EnterpriseProfileEntity loadRequiredWorkingProfile(EnterpriseEntity enterprise) {
        if (enterprise.getWorkingProfileId() == null) {
            throw new BizException(ErrorCode.ENTERPRISE_PROFILE_INCOMPLETE, "请先完善企业资料");
        }
        return enterpriseProfileRepository
                .findById(enterprise.getWorkingProfileId())
                .orElseThrow(
                        () -> new BizException(ErrorCode.ENTERPRISE_PROFILE_INCOMPLETE, "请先完善企业资料"));
    }

    private int nextVersion(UUID enterpriseId) {
        return enterpriseProfileRepository
                        .findTopByEnterpriseIdOrderByVersionNoDesc(enterpriseId)
                        .map(EnterpriseProfileEntity::getVersionNo)
                        .orElse(0)
                + 1;
    }

    private void applyProfileRequest(
            EnterpriseProfileEntity profile, EnterpriseProfileSaveRequest request) {
        profile.setName(request.name());
        profile.setShortName(request.shortName());
        profile.setSocialCreditCode(request.socialCreditCode());
        profile.setCompanyType(request.companyType());
        profile.setIndustry(request.industry());
        profile.setMainCategories(String.join(",", request.mainCategories()));
        profile.setProvince(request.province());
        profile.setCity(request.city());
        profile.setDistrict(request.district());
        profile.setAddress(request.address());
        profile.setSummary(request.summary());
        profile.setWebsite(blankToEmpty(request.website()));
        profile.setLogoUrl(blankToEmpty(request.logoUrl()));
        profile.setLicenseFileName(request.licenseFileName());
        profile.setLicensePreviewUrl(blankToEmpty(request.licensePreviewUrl()));
        profile.setContactName(request.contactName());
        profile.setContactTitle(blankToEmpty(request.contactTitle()));
        profile.setContactPhone(request.contactPhone());
        profile.setContactEmail(request.contactEmail());
        profile.setPublicContactName(request.publicContactName());
        profile.setPublicContactPhone(request.publicContactPhone());
        profile.setPublicContactEmail(request.publicContactEmail());
    }

    private void validateProfileCompleteness(EnterpriseProfileEntity profile) {
        if (isBlank(profile.getName())
                || isBlank(profile.getSocialCreditCode())
                || isBlank(profile.getCompanyType())
                || isBlank(profile.getIndustry())
                || isBlank(profile.getMainCategories())
                || isBlank(profile.getProvince())
                || isBlank(profile.getCity())
                || isBlank(profile.getDistrict())
                || isBlank(profile.getAddress())
                || isBlank(profile.getSummary())
                || isBlank(profile.getLicenseFileName())
                || isBlank(profile.getContactName())
                || isBlank(profile.getContactPhone())
                || isBlank(profile.getContactEmail())) {
            throw new BizException(ErrorCode.ENTERPRISE_PROFILE_INCOMPLETE, "请先完整填写企业入驻资料");
        }
    }

    private String writeSnapshot(EnterpriseEntity enterprise, EnterpriseProfileEntity profile) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("enterpriseId", enterprise.getId());
            payload.put("status", enterprise.getStatus().getCode());
            payload.put("profile", toCompanyProfileResponse(enterprise, profile));
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "企业资料快照序列化失败");
        }
    }

    public CompanyProfileResponse toCompanyProfileResponse(
            EnterpriseEntity enterprise, EnterpriseProfileEntity profile) {
        return new CompanyProfileResponse(
                enterprise.getId(),
                normalizeRequired(profile.getName()),
                normalizeOptional(profile.getShortName()),
                normalizeRequired(profile.getSocialCreditCode()),
                normalizeRequired(profile.getCompanyType()),
                normalizeRequired(profile.getIndustry()),
                splitCategories(profile.getMainCategories()),
                String.join(
                        " / ",
                        List.of(
                                blankToDash(profile.getProvince()),
                                blankToDash(profile.getCity()),
                                blankToDash(profile.getDistrict()))),
                normalizeRequired(profile.getAddress()),
                normalizeRequired(profile.getSummary()),
                normalizeOptional(profile.getWebsite()),
                normalizeOptional(profile.getLogoUrl()),
                normalizeRequired(profile.getLicenseFileName()),
                normalizeOptional(profile.getLicensePreviewUrl()),
                normalizeRequired(profile.getContactName()),
                normalizeOptional(profile.getContactTitle()),
                normalizeRequired(profile.getContactPhone()),
                normalizeRequired(profile.getContactEmail()),
                profile.isPublicContactName(),
                profile.isPublicContactPhone(),
                profile.isPublicContactEmail(),
                enterprise.getStatus().getCode(),
                enterprise.getLatestSubmissionAt(),
                enterprise.getJoinedAt(),
                enterprise.getLastReviewComment(),
                Math.toIntExact(productRepository.countByEnterpriseId(enterprise.getId())));
    }

    private List<String> splitCategories(String categories) {
        if (isBlank(categories)) {
            return Collections.emptyList();
        }
        return Arrays.stream(categories.split(","))
                .map(this::normalizeOptional)
                .filter(value -> value != null && !value.isBlank())
                .toList();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToEmpty(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? "" : normalized;
    }

    private String emptyToNull(String value) {
        return normalizeOptional(value);
    }

    private String blankToDash(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? "--" : normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isBlank() || normalized.chars().allMatch(ch -> ch == '?' || ch == '？')) {
            return null;
        }
        return normalized;
    }

    private String normalizeRequired(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? "" : normalized;
    }
}
