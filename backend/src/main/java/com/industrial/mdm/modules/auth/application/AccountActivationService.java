package com.industrial.mdm.modules.auth.application;

import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.common.security.UserRole;
import com.industrial.mdm.infrastructure.email.EmailProvider;
import com.industrial.mdm.modules.auth.domain.AccountStatus;
import com.industrial.mdm.modules.auth.dto.ActivationCompleteRequest;
import com.industrial.mdm.modules.auth.dto.ActivationCompleteResponse;
import com.industrial.mdm.modules.auth.dto.ActivationTokenPreviewResponse;
import com.industrial.mdm.modules.auth.repository.AccountActivationTokenEntity;
import com.industrial.mdm.modules.auth.repository.AccountActivationTokenRepository;
import com.industrial.mdm.modules.auth.repository.UserEntity;
import com.industrial.mdm.modules.auth.repository.UserRepository;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseEntity;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseProfileEntity;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseProfileRepository;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseRepository;
import com.industrial.mdm.modules.enterpriseReview.dto.AdminCompanyActivationPreviewResponse;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountActivationService {

    private final AccountActivationTokenRepository accountActivationTokenRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final EnterpriseProfileRepository enterpriseProfileRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailProvider emailProvider;
    private final String activationBaseUrl;

    public AccountActivationService(
            AccountActivationTokenRepository accountActivationTokenRepository,
            EnterpriseRepository enterpriseRepository,
            EnterpriseProfileRepository enterpriseProfileRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailProvider emailProvider,
            @Value("${mdm.auth.activation-base-url:http://localhost:5273/auth/activate}")
                    String activationBaseUrl) {
        this.accountActivationTokenRepository = accountActivationTokenRepository;
        this.enterpriseRepository = enterpriseRepository;
        this.enterpriseProfileRepository = enterpriseProfileRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailProvider = emailProvider;
        this.activationBaseUrl = activationBaseUrl;
    }

    @Transactional(readOnly = true)
    public ActivationTokenPreviewResponse previewActivation(String token) {
        AccountActivationTokenEntity activationToken = loadActiveToken(token);
        EnterpriseProfileEntity profile = loadEnterpriseProfile(activationToken.getEnterpriseId());
        return new ActivationTokenPreviewResponse(
                profile.getName(),
                profile.getContactName(),
                activationToken.getAccount(),
                activationToken.getPhone(),
                activationToken.getEmail(),
                activationToken.getExpiresAt());
    }

    @Transactional
    public ActivationCompleteResponse completeActivation(
            String token, ActivationCompleteRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "两次输入的密码不一致");
        }

        AccountActivationTokenEntity activationToken = loadActiveToken(token);
        EnterpriseEntity enterprise = loadEnterprise(activationToken.getEnterpriseId());
        EnterpriseProfileEntity profile = loadEnterpriseProfile(enterprise.getId());

        if (userRepository
                .findFirstByEnterpriseIdAndRole(enterprise.getId(), UserRole.ENTERPRISE_OWNER)
                .isPresent()) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "该企业已完成账号注册");
        }

        ensureUniqueAccount(
                activationToken.getAccount(), activationToken.getPhone(), activationToken.getEmail());

        UserEntity user = new UserEntity();
        user.setAccount(activationToken.getAccount());
        user.setPhone(activationToken.getPhone());
        user.setEmail(activationToken.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.ENTERPRISE_OWNER);
        user.setStatus(AccountStatus.ACTIVE);
        user.setEnterpriseId(enterprise.getId());
        user.setDisplayName(profile.getContactName());
        user.setOrganization(profile.getName());
        userRepository.save(user);

        activationToken.setUsedAt(OffsetDateTime.now());
        accountActivationTokenRepository.save(activationToken);

        return new ActivationCompleteResponse(
                "/auth/login", profile.getName(), activationToken.getAccount());
    }

    @Transactional
    public AdminCompanyActivationPreviewResponse issueActivationLinkIfNeeded(UUID enterpriseId) {
        if (userRepository.findFirstByEnterpriseIdAndRole(enterpriseId, UserRole.ENTERPRISE_OWNER).isPresent()) {
            return getActivationPreview(enterpriseId);
        }

        EnterpriseProfileEntity profile = loadEnterpriseProfile(enterpriseId);
        expireOutstandingTokens(enterpriseId);

        AccountActivationTokenEntity activationToken = new AccountActivationTokenEntity();
        activationToken.setEnterpriseId(enterpriseId);
        activationToken.setPhone(profile.getContactPhone().trim());
        activationToken.setEmail(profile.getContactEmail().trim().toLowerCase());
        activationToken.setAccount(selectLockedAccount(profile));
        activationToken.setTokenValue(generateTokenValue());
        activationToken.setExpiresAt(OffsetDateTime.now().plusDays(7));
        activationToken = accountActivationTokenRepository.save(activationToken);

        String activationLink = buildActivationLink(activationToken.getTokenValue());
        emailProvider.sendActivationLink(
                activationToken.getEmail(),
                "工业企业出海主数据平台账号激活",
                "您的企业入驻申请已审核通过，请通过邮件中的链接完成账号设置并登录平台。",
                activationLink);

        return toAdminPreview(activationToken);
    }

    @Transactional(readOnly = true)
    public AdminCompanyActivationPreviewResponse getActivationPreview(UUID enterpriseId) {
        return accountActivationTokenRepository
                .findTopByEnterpriseIdOrderByCreatedAtDesc(enterpriseId)
                .map(this::toAdminPreview)
                .orElse(null);
    }

    private void expireOutstandingTokens(UUID enterpriseId) {
        accountActivationTokenRepository.findByEnterpriseIdAndUsedAtIsNull(enterpriseId).forEach(
                token -> token.setUsedAt(OffsetDateTime.now()));
    }

    private AdminCompanyActivationPreviewResponse toAdminPreview(
            AccountActivationTokenEntity activationToken) {
        return new AdminCompanyActivationPreviewResponse(
                activationToken.getAccount(),
                activationToken.getEmail(),
                activationToken.getPhone(),
                activationToken.getUsedAt() == null
                        ? buildActivationLink(activationToken.getTokenValue())
                        : null,
                activationToken.getCreatedAt(),
                activationToken.getExpiresAt(),
                activationToken.getUsedAt());
    }

    private AccountActivationTokenEntity loadActiveToken(String token) {
        AccountActivationTokenEntity activationToken =
                accountActivationTokenRepository
                        .findTopByTokenValueOrderByCreatedAtDesc(token)
                        .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "激活链接不存在或已失效"));
        if (!activationToken.isActive()) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "激活链接已失效，请联系平台重新发送");
        }
        return activationToken;
    }

    private EnterpriseEntity loadEnterprise(UUID enterpriseId) {
        return enterpriseRepository
                .findById(enterpriseId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "企业不存在"));
    }

    private EnterpriseProfileEntity loadEnterpriseProfile(UUID enterpriseId) {
        return enterpriseProfileRepository
                .findTopByEnterpriseIdOrderByVersionNoDesc(enterpriseId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "企业资料不存在"));
    }

    private void ensureUniqueAccount(String account, String phone, String email) {
        if (userRepository.existsByAccountIgnoreCase(account)
                || userRepository.existsByPhone(phone)
                || userRepository.existsByEmailIgnoreCase(email)) {
            throw new BizException(ErrorCode.DUPLICATE_ACCOUNT, "该邮箱或手机号已被其他账号使用");
        }
    }

    private String selectLockedAccount(EnterpriseProfileEntity profile) {
        String email = profile.getContactEmail() == null ? "" : profile.getContactEmail().trim().toLowerCase();
        if (!email.isBlank()) {
            return email;
        }
        String phone = profile.getContactPhone() == null ? "" : profile.getContactPhone().trim();
        if (!phone.isBlank()) {
            return phone;
        }
        throw new BizException(ErrorCode.ENTERPRISE_PROFILE_INCOMPLETE, "缺少可用于注册的邮箱或手机号");
    }

    private String generateTokenValue() {
        return UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
    }

    private String buildActivationLink(String token) {
        return activationBaseUrl + "?token=" + token;
    }
}
