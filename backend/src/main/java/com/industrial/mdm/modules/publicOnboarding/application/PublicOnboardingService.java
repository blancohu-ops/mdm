package com.industrial.mdm.modules.publicOnboarding.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.common.security.UserRole;
import com.industrial.mdm.modules.auth.repository.UserRepository;
import com.industrial.mdm.modules.enterprise.domain.EnterpriseStatus;
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
import com.industrial.mdm.modules.publicOnboarding.dto.PublicOnboardingApplicationRequest;
import com.industrial.mdm.modules.publicOnboarding.dto.PublicOnboardingApplicationResponse;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicOnboardingService {

    private static final String DEFAULT_COMPANY_TYPE = "待完善";
    private static final String DEFAULT_REGION = "待完善";
    private static final String DEFAULT_SUMMARY = "待企业激活账号后补充完整企业简介。";
    private static final String DEFAULT_LICENSE_FILE = "待上传营业执照";

    private final EnterpriseRepository enterpriseRepository;
    private final EnterpriseProfileRepository enterpriseProfileRepository;
    private final EnterpriseSubmissionRecordRepository enterpriseSubmissionRecordRepository;
    private final EnterpriseSubmissionSnapshotRepository enterpriseSubmissionSnapshotRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public PublicOnboardingService(
            EnterpriseRepository enterpriseRepository,
            EnterpriseProfileRepository enterpriseProfileRepository,
            EnterpriseSubmissionRecordRepository enterpriseSubmissionRecordRepository,
            EnterpriseSubmissionSnapshotRepository enterpriseSubmissionSnapshotRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper) {
        this.enterpriseRepository = enterpriseRepository;
        this.enterpriseProfileRepository = enterpriseProfileRepository;
        this.enterpriseSubmissionRecordRepository = enterpriseSubmissionRecordRepository;
        this.enterpriseSubmissionSnapshotRepository = enterpriseSubmissionSnapshotRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PublicOnboardingApplicationResponse submit(PublicOnboardingApplicationRequest request) {
        String email = request.email().trim().toLowerCase();
        String phone = request.phone().trim();

        ensureNoDuplicateRequest(request.companyName().trim(), email, phone);

        EnterpriseEntity enterprise = new EnterpriseEntity();
        enterprise.setName(request.companyName().trim());
        enterprise.setStatus(EnterpriseStatus.PENDING_REVIEW);
        enterprise.setLatestSubmissionAt(OffsetDateTime.now());
        enterprise = enterpriseRepository.save(enterprise);

        EnterpriseProfileEntity profile = new EnterpriseProfileEntity();
        profile.setEnterpriseId(enterprise.getId());
        profile.setVersionNo(1);
        profile.setName(request.companyName().trim());
        profile.setShortName("");
        profile.setSocialCreditCode("");
        profile.setCompanyType(DEFAULT_COMPANY_TYPE);
        profile.setIndustry(request.industry().trim());
        profile.setMainCategories("");
        profile.setProvince(DEFAULT_REGION);
        profile.setCity(DEFAULT_REGION);
        profile.setDistrict(DEFAULT_REGION);
        profile.setAddress(DEFAULT_REGION);
        profile.setSummary(DEFAULT_SUMMARY);
        profile.setWebsite("");
        profile.setLogoUrl("");
        profile.setLicenseFileName(DEFAULT_LICENSE_FILE);
        profile.setLicensePreviewUrl("");
        profile.setContactName(request.contactName().trim());
        profile.setContactTitle("");
        profile.setContactPhone(phone);
        profile.setContactEmail(email);
        profile.setPublicContactName(true);
        profile.setPublicContactPhone(false);
        profile.setPublicContactEmail(false);
        profile = enterpriseProfileRepository.save(profile);

        enterprise.setWorkingProfileId(profile.getId());
        enterpriseRepository.save(enterprise);

        EnterpriseSubmissionRecordEntity submission = new EnterpriseSubmissionRecordEntity();
        submission.setEnterpriseId(enterprise.getId());
        submission.setSubmissionType(EnterpriseSubmissionType.ONBOARDING);
        submission.setStatus(EnterpriseSubmissionStatus.PENDING_REVIEW);
        submission.setSubmissionName(profile.getName());
        submission.setSubmissionSocialCreditCode("");
        submission.setSubmissionIndustry(profile.getIndustry());
        submission.setSubmissionContactName(profile.getContactName());
        submission.setSubmissionContactPhone(profile.getContactPhone());
        submission.setSubmittedBy(null);
        submission.setSubmittedAt(enterprise.getLatestSubmissionAt());
        submission = enterpriseSubmissionRecordRepository.save(submission);

        EnterpriseSubmissionSnapshotEntity snapshot = new EnterpriseSubmissionSnapshotEntity();
        snapshot.setEnterpriseId(enterprise.getId());
        snapshot.setSubmissionId(submission.getId());
        snapshot.setPayloadJson(writeSnapshot(enterprise, profile, request));
        snapshot = enterpriseSubmissionSnapshotRepository.save(snapshot);

        submission.setSnapshotId(snapshot.getId());
        enterpriseSubmissionRecordRepository.save(submission);

        return new PublicOnboardingApplicationResponse(
                enterprise.getId(),
                enterprise.getStatus().getCode(),
                submission.getSubmittedAt(),
                profile.getName(),
                profile.getContactEmail(),
                profile.getContactPhone());
    }

    private void ensureNoDuplicateRequest(String companyName, String email, String phone) {
        if (userRepository.existsByPhone(phone)
                || userRepository.existsByEmailIgnoreCase(email)
                || userRepository.existsByAccountIgnoreCase(email)
                || userRepository.existsByAccountIgnoreCase(phone)) {
            throw new BizException(ErrorCode.DUPLICATE_ACCOUNT, "该手机号或邮箱已存在可登录账号");
        }
        boolean duplicatePending =
                enterpriseProfileRepository
                        .findFirstByContactEmailIgnoreCaseOrContactPhone(email, phone)
                        .flatMap(profile -> enterpriseRepository.findById(profile.getEnterpriseId()))
                        .filter(this::isBlockingExistingApplication)
                        .isPresent();
        if (duplicatePending) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "该邮箱或手机号已有待审核或待激活的入驻申请");
        }

        boolean duplicateCompanyPending =
                enterpriseProfileRepository
                        .findFirstByNameIgnoreCase(companyName)
                        .flatMap(profile -> enterpriseRepository.findById(profile.getEnterpriseId()))
                        .filter(this::isBlockingExistingApplication)
                        .isPresent();
        if (duplicateCompanyPending) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "该企业已有待审核或待激活的入驻申请");
        }
    }

    private boolean isBlockingExistingApplication(EnterpriseEntity enterprise) {
        if (enterprise.getStatus() == EnterpriseStatus.PENDING_REVIEW) {
            return true;
        }
        if (enterprise.getStatus() != EnterpriseStatus.APPROVED) {
            return false;
        }
        return userRepository
                .findFirstByEnterpriseIdAndRole(enterprise.getId(), UserRole.ENTERPRISE_OWNER)
                .isEmpty();
    }

    private String writeSnapshot(
            EnterpriseEntity enterprise,
            EnterpriseProfileEntity profile,
            PublicOnboardingApplicationRequest request) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("enterpriseId", enterprise.getId());
            payload.put("status", enterprise.getStatus().getCode());
            payload.put(
                    "application",
                    Map.of(
                            "companyName", profile.getName(),
                            "contactName", profile.getContactName(),
                            "phone", profile.getContactPhone(),
                            "email", profile.getContactEmail(),
                            "industry", profile.getIndustry(),
                            "acceptedAgreement", request.acceptedAgreement()));
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "公开入驻申请快照保存失败");
        }
    }
}
