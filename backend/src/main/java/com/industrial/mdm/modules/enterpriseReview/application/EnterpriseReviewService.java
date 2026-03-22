package com.industrial.mdm.modules.enterpriseReview.application;

import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.common.security.UserRole;
import com.industrial.mdm.modules.enterprise.application.EnterpriseService;
import com.industrial.mdm.modules.enterprise.domain.EnterpriseStatus;
import com.industrial.mdm.modules.enterprise.dto.CompanyProfileResponse;
import com.industrial.mdm.modules.enterprise.dto.EnterpriseLatestSubmissionResponse;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseEntity;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseProfileEntity;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseProfileRepository;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseRepository;
import com.industrial.mdm.modules.enterpriseReview.domain.EnterpriseSubmissionStatus;
import com.industrial.mdm.modules.enterpriseReview.dto.AdminCompanyListResponse;
import com.industrial.mdm.modules.enterpriseReview.dto.AdminCompanyReviewDecisionRequest;
import com.industrial.mdm.modules.enterpriseReview.dto.AdminCompanyReviewDetailResponse;
import com.industrial.mdm.modules.enterpriseReview.repository.CompanyListQueryRepository;
import com.industrial.mdm.modules.enterpriseReview.repository.EnterpriseSubmissionRecordEntity;
import com.industrial.mdm.modules.enterpriseReview.repository.EnterpriseSubmissionRecordRepository;
import com.industrial.mdm.modules.message.application.MessageService;
import com.industrial.mdm.modules.message.domain.MessageType;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnterpriseReviewService {

    private final EnterpriseRepository enterpriseRepository;
    private final EnterpriseProfileRepository enterpriseProfileRepository;
    private final EnterpriseSubmissionRecordRepository enterpriseSubmissionRecordRepository;
    private final CompanyListQueryRepository companyListQueryRepository;
    private final EnterpriseService enterpriseService;
    private final MessageService messageService;

    public EnterpriseReviewService(
            EnterpriseRepository enterpriseRepository,
            EnterpriseProfileRepository enterpriseProfileRepository,
            EnterpriseSubmissionRecordRepository enterpriseSubmissionRecordRepository,
            CompanyListQueryRepository companyListQueryRepository,
            EnterpriseService enterpriseService,
            MessageService messageService) {
        this.enterpriseRepository = enterpriseRepository;
        this.enterpriseProfileRepository = enterpriseProfileRepository;
        this.enterpriseSubmissionRecordRepository = enterpriseSubmissionRecordRepository;
        this.companyListQueryRepository = companyListQueryRepository;
        this.enterpriseService = enterpriseService;
        this.messageService = messageService;
    }

    @Transactional(readOnly = true)
    public AdminCompanyListResponse listReviews(
            String keyword, String industry, String status, int page, int pageSize) {
        var pageResult =
                companyListQueryRepository.findReviewCompanyIds(keyword, industry, status, page, pageSize);
        return new AdminCompanyListResponse(
                loadCompaniesInOrder(pageResult.items()),
                companyListQueryRepository.findIndustries(),
                pageResult.total(),
                pageResult.page(),
                pageResult.pageSize());
    }

    @Transactional(readOnly = true)
    public AdminCompanyListResponse listManagementCompanies(
            String keyword, String industry, String status, int page, int pageSize) {
        var pageResult =
                companyListQueryRepository.findManagementCompanyIds(
                        keyword, industry, status, page, pageSize);
        return new AdminCompanyListResponse(
                loadCompaniesInOrder(pageResult.items()),
                companyListQueryRepository.findIndustries(),
                pageResult.total(),
                pageResult.page(),
                pageResult.pageSize());
    }

    @Transactional(readOnly = true)
    public AdminCompanyReviewDetailResponse getReviewDetail(UUID enterpriseId) {
        EnterpriseEntity enterprise = findEnterprise(enterpriseId);
        EnterpriseSubmissionRecordEntity latestSubmission =
                enterpriseSubmissionRecordRepository
                        .findTopByEnterpriseIdOrderBySubmittedAtDesc(enterpriseId)
                        .orElse(null);
        return new AdminCompanyReviewDetailResponse(
                toCompanyProfile(enterprise),
                latestSubmission == null
                        ? null
                        : new EnterpriseLatestSubmissionResponse(
                                latestSubmission.getId(),
                                latestSubmission.getSubmissionType().getCode(),
                                latestSubmission.getStatus().getCode(),
                                latestSubmission.getSubmittedAt(),
                                latestSubmission.getReviewComment()));
    }

    @Transactional
    public AdminCompanyReviewDetailResponse approve(
            UUID enterpriseId, AdminCompanyReviewDecisionRequest request, AuthenticatedUser reviewer) {
        assertReviewRole(reviewer);
        EnterpriseEntity enterprise = findEnterprise(enterpriseId);
        EnterpriseSubmissionRecordEntity submission = findPendingSubmission(enterpriseId);
        if (enterprise.getWorkingProfileId() == null) {
            throw new BizException(ErrorCode.ENTERPRISE_PROFILE_INCOMPLETE, "缺少待审核企业资料");
        }

        submission.setStatus(EnterpriseSubmissionStatus.APPROVED);
        submission.setReviewedAt(OffsetDateTime.now());
        submission.setReviewedBy(reviewer.userId());
        submission.setReviewComment(request.reviewComment());
        submission.setInternalNote(request.internalNote());
        enterpriseSubmissionRecordRepository.save(submission);

        enterprise.setStatus(EnterpriseStatus.APPROVED);
        enterprise.setCurrentProfileId(enterprise.getWorkingProfileId());
        enterprise.setJoinedAt(
                enterprise.getJoinedAt() == null ? LocalDate.now() : enterprise.getJoinedAt());
        enterprise.setLastReviewComment(request.reviewComment());
        enterpriseRepository.save(enterprise);
        messageService.sendToEnterpriseUsers(
                enterprise.getId(),
                MessageType.REVIEW,
                "企业入驻审核通过",
                "企业入驻申请已审核通过，可以开始录入和提交产品。",
                "平台已审核通过你的企业资料，建议尽快完善产品主数据并提交产品审核。",
                "enterprise",
                enterprise.getId());
        return getReviewDetail(enterpriseId);
    }

    @Transactional
    public AdminCompanyReviewDetailResponse reject(
            UUID enterpriseId, AdminCompanyReviewDecisionRequest request, AuthenticatedUser reviewer) {
        assertReviewRole(reviewer);
        if (request.reviewComment() == null || request.reviewComment().isBlank()) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "驳回原因不能为空");
        }
        EnterpriseEntity enterprise = findEnterprise(enterpriseId);
        EnterpriseSubmissionRecordEntity submission = findPendingSubmission(enterpriseId);

        submission.setStatus(EnterpriseSubmissionStatus.REJECTED);
        submission.setReviewedAt(OffsetDateTime.now());
        submission.setReviewedBy(reviewer.userId());
        submission.setReviewComment(request.reviewComment());
        submission.setInternalNote(request.internalNote());
        enterpriseSubmissionRecordRepository.save(submission);

        enterprise.setStatus(EnterpriseStatus.REJECTED);
        enterprise.setLastReviewComment(request.reviewComment());
        enterpriseRepository.save(enterprise);
        messageService.sendToEnterpriseUsers(
                enterprise.getId(),
                MessageType.REVIEW,
                "企业入驻已驳回待修改",
                "企业资料未通过审核，请根据驳回原因修改后重新提交。",
                request.reviewComment(),
                "enterprise",
                enterprise.getId());
        return getReviewDetail(enterpriseId);
    }

    @Transactional
    public CompanyProfileResponse freeze(UUID enterpriseId, AuthenticatedUser operator) {
        assertOperationsAdmin(operator);
        EnterpriseEntity enterprise = findEnterprise(enterpriseId);
        if (enterprise.getStatus() == EnterpriseStatus.UNSUBMITTED) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "未提交的企业不能冻结");
        }
        if (enterprise.getStatus() == EnterpriseStatus.FROZEN) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "企业已处于冻结状态");
        }

        enterprise.setStatus(EnterpriseStatus.FROZEN);
        enterpriseRepository.save(enterprise);
        messageService.sendToEnterpriseUsers(
                enterprise.getId(),
                MessageType.SYSTEM,
                "企业账号已被冻结",
                "平台已冻结当前企业资料，请联系平台管理员了解详情。",
                "冻结期间你仍可登录后台查看历史记录，但无法继续提交资料或产品审核。",
                "enterprise",
                enterprise.getId());
        return toCompanyProfile(enterprise);
    }

    @Transactional
    public CompanyProfileResponse restore(UUID enterpriseId, AuthenticatedUser operator) {
        assertOperationsAdmin(operator);
        EnterpriseEntity enterprise = findEnterprise(enterpriseId);
        if (enterprise.getStatus() != EnterpriseStatus.FROZEN) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "仅冻结状态的企业可执行恢复");
        }

        enterprise.setStatus(EnterpriseStatus.APPROVED);
        enterpriseRepository.save(enterprise);
        messageService.sendToEnterpriseUsers(
                enterprise.getId(),
                MessageType.SYSTEM,
                "企业账号已恢复",
                "平台已恢复当前企业状态，你可以继续维护资料并提交产品审核。",
                "建议先检查企业资料和产品状态，再继续后续业务操作。",
                "enterprise",
                enterprise.getId());
        return toCompanyProfile(enterprise);
    }

    private void assertReviewRole(AuthenticatedUser reviewer) {
        if (reviewer == null
                || (reviewer.role() != UserRole.REVIEWER
                        && reviewer.role() != UserRole.OPERATIONS_ADMIN)) {
            throw new BizException(ErrorCode.FORBIDDEN, "当前账号无企业审核权限");
        }
    }

    private void assertOperationsAdmin(AuthenticatedUser operator) {
        if (operator == null || operator.role() != UserRole.OPERATIONS_ADMIN) {
            throw new BizException(ErrorCode.FORBIDDEN, "当前账号没有企业管理权限");
        }
    }

    private EnterpriseEntity findEnterprise(UUID enterpriseId) {
        return enterpriseRepository
                .findById(enterpriseId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "企业不存在"));
    }

    private EnterpriseSubmissionRecordEntity findPendingSubmission(UUID enterpriseId) {
        return enterpriseSubmissionRecordRepository
                .findTopByEnterpriseIdAndStatusOrderBySubmittedAtDesc(
                        enterpriseId, EnterpriseSubmissionStatus.PENDING_REVIEW)
                .orElseThrow(
                        () -> new BizException(ErrorCode.STATE_CONFLICT, "当前没有待审核企业提交记录"));
    }

    private CompanyProfileResponse toCompanyProfile(EnterpriseEntity enterprise) {
        UUID profileId =
                enterprise.getWorkingProfileId() != null
                        ? enterprise.getWorkingProfileId()
                        : enterprise.getCurrentProfileId();
        if (profileId == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "企业资料不存在");
        }
        EnterpriseProfileEntity profile =
                enterpriseProfileRepository
                        .findById(profileId)
                        .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "企业资料不存在"));
        return enterpriseService.toCompanyProfileResponse(enterprise, profile);
    }

    private List<CompanyProfileResponse> loadCompaniesInOrder(List<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<UUID, EnterpriseEntity> enterpriseMap =
                enterpriseRepository.findAllById(ids).stream()
                        .collect(Collectors.toMap(EnterpriseEntity::getId, Function.identity()));
        return ids.stream()
                .map(enterpriseMap::get)
                .filter(java.util.Objects::nonNull)
                .map(this::toCompanyProfile)
                .toList();
    }
}
