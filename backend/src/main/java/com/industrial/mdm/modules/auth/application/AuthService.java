package com.industrial.mdm.modules.auth.application;

import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.common.security.JwtTokenService;
import com.industrial.mdm.common.security.TokenHashUtils;
import com.industrial.mdm.common.security.UserRole;
import com.industrial.mdm.infrastructure.sms.SmsProvider;
import com.industrial.mdm.modules.auth.domain.AccountStatus;
import com.industrial.mdm.modules.auth.domain.LoginResult;
import com.industrial.mdm.modules.auth.domain.SmsCodePurpose;
import com.industrial.mdm.modules.auth.dto.AccountSettingsResponse;
import com.industrial.mdm.modules.auth.dto.AccountSettingsUpdateRequest;
import com.industrial.mdm.modules.auth.dto.AuthMeResponse;
import com.industrial.mdm.modules.auth.dto.LoginRequest;
import com.industrial.mdm.modules.auth.dto.LoginResponse;
import com.industrial.mdm.modules.auth.dto.RefreshTokenRequest;
import com.industrial.mdm.modules.auth.dto.RegisterRequest;
import com.industrial.mdm.modules.auth.dto.RegisterResponse;
import com.industrial.mdm.modules.auth.dto.ResetPasswordRequest;
import com.industrial.mdm.modules.auth.dto.SmsCodeRequest;
import com.industrial.mdm.modules.auth.dto.SmsCodeResponse;
import com.industrial.mdm.modules.auth.repository.LoginLogEntity;
import com.industrial.mdm.modules.auth.repository.LoginLogRepository;
import com.industrial.mdm.modules.auth.repository.RefreshTokenEntity;
import com.industrial.mdm.modules.auth.repository.RefreshTokenRepository;
import com.industrial.mdm.modules.auth.repository.SmsCodeEntity;
import com.industrial.mdm.modules.auth.repository.SmsCodeRepository;
import com.industrial.mdm.modules.auth.repository.UserEntity;
import com.industrial.mdm.modules.auth.repository.UserRepository;
import com.industrial.mdm.modules.enterprise.domain.EnterpriseStatus;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseEntity;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseProfileEntity;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseProfileRepository;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseRepository;
import com.industrial.mdm.modules.iam.application.AuthorizationProfile;
import com.industrial.mdm.modules.iam.application.AuthorizationService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String DEV_SMS_CODE = "123456";
    private static final String DEFAULT_LICENSE_FILE = "待上传营业执照.pdf";
    private static final String DEFAULT_LICENSE_PREVIEW =
            "https://images.unsplash.com/photo-1516321497487-e288fb19713f?auto=format&fit=crop&w=900&q=80";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginLogRepository loginLogRepository;
    private final SmsCodeRepository smsCodeRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final EnterpriseProfileRepository enterpriseProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final SmsProvider smsProvider;
    private final AuthorizationService authorizationService;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            LoginLogRepository loginLogRepository,
            SmsCodeRepository smsCodeRepository,
            EnterpriseRepository enterpriseRepository,
            EnterpriseProfileRepository enterpriseProfileRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            SmsProvider smsProvider,
            AuthorizationService authorizationService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.loginLogRepository = loginLogRepository;
        this.smsCodeRepository = smsCodeRepository;
        this.enterpriseRepository = enterpriseRepository;
        this.enterpriseProfileRepository = enterpriseProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.smsProvider = smsProvider;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public SmsCodeResponse sendSmsCode(SmsCodeRequest request) {
        SmsCodePurpose purpose = SmsCodePurpose.fromCode(request.purpose());
        SmsCodeEntity smsCode = new SmsCodeEntity();
        smsCode.setPhone(request.phone());
        smsCode.setPurpose(purpose);
        smsCode.setCode(DEV_SMS_CODE);
        smsCode.setExpiresAt(OffsetDateTime.now().plusMinutes(5));
        smsCodeRepository.save(smsCode);
        smsProvider.sendCode(request.phone(), DEV_SMS_CODE);
        return new SmsCodeResponse(60);
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        verifySmsCode(request.phone(), SmsCodePurpose.REGISTER, request.smsCode());
        ensureUniqueAccount(request.phone(), request.email());

        EnterpriseEntity enterprise = new EnterpriseEntity();
        enterprise.setName(request.companyName());
        enterprise.setStatus(EnterpriseStatus.UNSUBMITTED);
        enterprise = enterpriseRepository.save(enterprise);

        EnterpriseProfileEntity workingProfile = new EnterpriseProfileEntity();
        workingProfile.setEnterpriseId(enterprise.getId());
        workingProfile.setVersionNo(1);
        workingProfile.setName(request.companyName());
        workingProfile.setShortName(null);
        workingProfile.setSocialCreditCode("");
        workingProfile.setCompanyType("");
        workingProfile.setIndustry("");
        workingProfile.setMainCategories("");
        workingProfile.setProvince("");
        workingProfile.setCity("");
        workingProfile.setDistrict("");
        workingProfile.setAddress("");
        workingProfile.setSummary("");
        workingProfile.setWebsite("");
        workingProfile.setLogoUrl("");
        workingProfile.setLicenseFileName(DEFAULT_LICENSE_FILE);
        workingProfile.setLicensePreviewUrl(DEFAULT_LICENSE_PREVIEW);
        workingProfile.setContactName(request.contactName());
        workingProfile.setContactTitle("");
        workingProfile.setContactPhone(request.phone());
        workingProfile.setContactEmail(request.email().trim().toLowerCase());
        workingProfile.setPublicContactName(true);
        workingProfile.setPublicContactPhone(true);
        workingProfile.setPublicContactEmail(false);
        workingProfile = enterpriseProfileRepository.save(workingProfile);

        enterprise.setWorkingProfileId(workingProfile.getId());
        enterpriseRepository.save(enterprise);

        UserEntity user = new UserEntity();
        user.setAccount(request.phone());
        user.setPhone(request.phone());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.ENTERPRISE_OWNER);
        user.setStatus(AccountStatus.ACTIVE);
        user.setEnterpriseId(enterprise.getId());
        user.setDisplayName(request.contactName());
        user.setOrganization(request.companyName());
        userRepository.save(user);

        return new RegisterResponse("/enterprise/onboarding/apply", request.companyName());
    }

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String normalizedAccount = request.account().trim().toLowerCase();
        UserEntity user =
                userRepository
                        .findFirstByAccountIgnoreCaseOrPhoneOrEmailIgnoreCase(
                                normalizedAccount, normalizedAccount, normalizedAccount)
                        .orElseThrow(
                                () -> {
                                    logLogin(null, normalizedAccount, LoginResult.FAILURE, httpRequest, "account not found");
                                    return new BizException(
                                            ErrorCode.INVALID_CREDENTIALS, "账号或密码错误");
                                });

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            logLogin(user, normalizedAccount, LoginResult.FAILURE, httpRequest, "password mismatch");
            throw new BizException(ErrorCode.INVALID_CREDENTIALS, "账号或密码错误");
        }
        if (!user.getStatus().isActive()) {
            logLogin(user, normalizedAccount, LoginResult.FAILURE, httpRequest, "account frozen");
            throw new BizException(ErrorCode.FORBIDDEN, "账号已被冻结");
        }

        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        AuthenticatedUser principal = toPrincipal(user);
        if (principal.authzVersion() != normalizeAuthzVersion(user.getAuthzVersion())) {
            throw new BizException(ErrorCode.REFRESH_TOKEN_INVALID, "refresh token 鏃犳晥");
        }
        revokeActiveRefreshTokens(user.getId());
        JwtTokenService.TokenPair tokenPair = jwtTokenService.issueTokens(principal);
        saveRefreshToken(user.getId(), tokenPair);
        logLogin(user, normalizedAccount, LoginResult.SUCCESS, httpRequest, null);
        return buildLoginResponse(principal, tokenPair);
    }

    @Transactional(readOnly = true)
    public AuthMeResponse me(AuthenticatedUser currentUser) {
        UserEntity user = loadCurrentUser(currentUser);
        AuthenticatedUser principal = toPrincipal(user);
        AuthorizationProfile authorizationProfile = authorizationService.getProfile(principal);
        return new AuthMeResponse(
                principal.userId(),
                principal.role().getCode(),
                principal.enterpriseId(),
                principal.serviceProviderId(),
                principal.displayName(),
                principal.organization(),
                authorizationProfile.permissions().stream()
                        .map(permission -> permission.getCode())
                        .sorted()
                        .toList(),
                authorizationProfile.dataScopes().stream()
                        .map(dataScope -> dataScope.getCode())
                        .sorted()
                        .toList(),
                authorizationProfile.capabilities().stream()
                        .map(capability -> capability.getCode())
                        .sorted(Comparator.naturalOrder())
                        .toList());
    }

    @Transactional(readOnly = true)
    public AccountSettingsResponse getAccountSettings(AuthenticatedUser currentUser) {
        UserEntity user = loadCurrentUser(currentUser);
        return new AccountSettingsResponse(
                normalizeRequired(user.getAccount()),
                normalizeRequired(user.getPhone()),
                normalizeRequired(user.getEmail()));
    }

    @Transactional
    public AccountSettingsResponse updateAccountSettings(
            AuthenticatedUser currentUser, AccountSettingsUpdateRequest request) {
        UserEntity user = loadCurrentUser(currentUser);
        String nextPhone = request.phone().trim();
        String nextEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByPhoneAndIdNot(nextPhone, user.getId())) {
            throw new BizException(ErrorCode.DUPLICATE_ACCOUNT, "手机号已被其他账号使用");
        }
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(nextEmail, user.getId())) {
            throw new BizException(ErrorCode.DUPLICATE_ACCOUNT, "邮箱已被其他账号使用");
        }

        boolean passwordChangeRequested =
                hasText(request.currentPassword())
                        || hasText(request.password())
                        || hasText(request.confirmPassword());
        if (passwordChangeRequested) {
            if (!hasText(request.currentPassword())) {
                throw new BizException(ErrorCode.INVALID_REQUEST, "请输入当前密码");
            }
            if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
                throw new BizException(ErrorCode.INVALID_CREDENTIALS, "当前密码不正确");
            }
            if (!hasText(request.password())) {
                throw new BizException(ErrorCode.INVALID_REQUEST, "请输入新密码");
            }
            if (request.password().trim().length() < 8) {
                throw new BizException(ErrorCode.INVALID_REQUEST, "新密码长度不能少于 8 位");
            }
            if (!request.password().equals(request.confirmPassword())) {
                throw new BizException(ErrorCode.INVALID_REQUEST, "两次输入的新密码不一致");
            }
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            revokeActiveRefreshTokens(user.getId());
        }

        String currentAccount = normalizeRequired(user.getAccount()).toLowerCase();
        String currentPhone = normalizeRequired(user.getPhone()).toLowerCase();
        String currentEmail = normalizeRequired(user.getEmail()).toLowerCase();
        user.setPhone(nextPhone);
        user.setEmail(nextEmail);
        if (currentAccount.equals(currentPhone)) {
            user.setAccount(nextPhone);
        } else if (currentAccount.equals(currentEmail)) {
            user.setAccount(nextEmail);
        }
        userRepository.save(user);

        return new AccountSettingsResponse(
                normalizeRequired(user.getAccount()),
                normalizeRequired(user.getPhone()),
                normalizeRequired(user.getEmail()));
    }

    @Transactional
    public LoginResponse refresh(RefreshTokenRequest request) {
        AuthenticatedUser principal;
        try {
            principal = jwtTokenService.parseRefreshToken(request.refreshToken());
        } catch (RuntimeException exception) {
            throw new BizException(ErrorCode.REFRESH_TOKEN_INVALID, "refresh token 无效");
        }

        RefreshTokenEntity refreshToken =
                refreshTokenRepository
                        .findByTokenHash(TokenHashUtils.sha256(request.refreshToken()))
                        .filter(RefreshTokenEntity::isActive)
                        .orElseThrow(
                                () ->
                                        new BizException(
                                                ErrorCode.REFRESH_TOKEN_INVALID, "refresh token 无效"));
        refreshToken.setRevokedAt(OffsetDateTime.now());
        refreshTokenRepository.save(refreshToken);

        UserEntity user =
                userRepository
                        .findById(principal.userId())
                        .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED, "用户不存在"));
        revokeActiveRefreshTokens(user.getId());
        JwtTokenService.TokenPair tokenPair = jwtTokenService.issueTokens(toPrincipal(user));
        saveRefreshToken(user.getId(), tokenPair);
        return buildLoginResponse(toPrincipal(user), tokenPair);
    }

    @Transactional
    public Map<String, String> resetPassword(ResetPasswordRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "两次输入的密码不一致");
        }
        verifySmsCode(request.phone(), SmsCodePurpose.RESET_PASSWORD, request.smsCode());
        UserEntity user =
                userRepository
                        .findByPhone(request.phone())
                        .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "未找到该手机号对应账号"));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);
        revokeActiveRefreshTokens(user.getId());
        return Map.of("redirectPath", "/auth/login");
    }

    private void ensureUniqueAccount(String phone, String email) {
        if (userRepository.existsByPhone(phone)
                || userRepository.existsByEmailIgnoreCase(email)
                || userRepository.existsByAccountIgnoreCase(phone)) {
            throw new BizException(ErrorCode.DUPLICATE_ACCOUNT, "手机号或邮箱已注册");
        }
    }

    private void verifySmsCode(String phone, SmsCodePurpose purpose, String code) {
        SmsCodeEntity smsCode =
                smsCodeRepository
                        .findTopByPhoneAndPurposeAndCodeAndUsedAtIsNullOrderByCreatedAtDesc(
                                phone, purpose, code)
                        .orElseThrow(
                                () -> new BizException(ErrorCode.SMS_CODE_INVALID, "验证码错误"));
        if (smsCode.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BizException(ErrorCode.SMS_CODE_EXPIRED, "验证码已过期");
        }
        smsCode.setUsedAt(OffsetDateTime.now());
        smsCodeRepository.save(smsCode);
    }

    private UserEntity loadCurrentUser(AuthenticatedUser currentUser) {
        if (currentUser == null || currentUser.userId() == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "当前登录状态无效");
        }
        return userRepository
                .findById(currentUser.userId())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED, "当前账号不存在"));
    }

    private void saveRefreshToken(UUID userId, JwtTokenService.TokenPair tokenPair) {
        RefreshTokenEntity refreshToken = new RefreshTokenEntity();
        refreshToken.setUserId(userId);
        refreshToken.setTokenHash(TokenHashUtils.sha256(tokenPair.refreshToken()));
        refreshToken.setExpiresAt(tokenPair.refreshTokenExpiresAt());
        refreshTokenRepository.save(refreshToken);
    }

    private void revokeActiveRefreshTokens(UUID userId) {
        refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId).forEach(
                token -> token.setRevokedAt(OffsetDateTime.now()));
    }

    private AuthenticatedUser toPrincipal(UserEntity user) {
        return new AuthenticatedUser(
                user.getId(),
                user.getRole(),
                user.getEnterpriseId(),
                user.getServiceProviderId(),
                user.getDisplayName(),
                user.getOrganization(),
                normalizeAuthzVersion(user.getAuthzVersion()));
    }

    private LoginResponse buildLoginResponse(
            AuthenticatedUser principal, JwtTokenService.TokenPair tokenPair) {
        return new LoginResponse(
                principal.role().getCode(),
                switch (principal.role()) {
                    case ENTERPRISE_OWNER -> "/enterprise/dashboard";
                    case PROVIDER_OWNER -> "/provider/dashboard";
                    case REVIEWER, OPERATIONS_ADMIN -> "/admin/overview";
                },
                principal.displayName(),
                principal.organization(),
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tokenPair.accessTokenExpiresAt(),
                tokenPair.refreshTokenExpiresAt());
    }

    private void logLogin(
            UserEntity user,
            String account,
            LoginResult result,
            HttpServletRequest request,
            String failureReason) {
        LoginLogEntity log = new LoginLogEntity();
        if (user != null) {
            log.setUserId(user.getId());
            log.setRole(user.getRole());
            log.setEnterpriseId(user.getEnterpriseId());
        }
        log.setAccount(account);
        log.setResult(result);
        log.setClientIp(request.getRemoteAddr());
        log.setUserAgent(request.getHeader("User-Agent"));
        log.setFailureReason(failureReason);
        loginLogRepository.save(log);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizeRequired(String value) {
        return value == null ? "" : value.trim();
    }

    private int normalizeAuthzVersion(Integer authzVersion) {
        return authzVersion == null ? 0 : authzVersion;
    }
}
