package com.industrial.mdm.modules.serviceProvider.application;

import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.common.security.UserRole;
import com.industrial.mdm.infrastructure.email.EmailProvider;
import com.industrial.mdm.modules.auth.domain.AccountStatus;
import com.industrial.mdm.modules.auth.repository.UserEntity;
import com.industrial.mdm.modules.auth.repository.UserRepository;
import com.industrial.mdm.modules.iam.application.AuthorizationService;
import com.industrial.mdm.modules.iam.domain.permission.PermissionCode;
import com.industrial.mdm.modules.serviceProvider.domain.ServiceProviderApplicationStatus;
import com.industrial.mdm.modules.serviceProvider.domain.ServiceProviderStatus;
import com.industrial.mdm.modules.serviceProvider.dto.ProviderActivationCompleteRequest;
import com.industrial.mdm.modules.serviceProvider.dto.ProviderActivationCompleteResponse;
import com.industrial.mdm.modules.serviceProvider.dto.ProviderActivationPreviewResponse;
import com.industrial.mdm.modules.serviceProvider.dto.ProviderActivationTokenPreviewResponse;
import com.industrial.mdm.modules.serviceProvider.dto.ProviderReviewDecisionRequest;
import com.industrial.mdm.modules.serviceProvider.dto.PublicProviderOnboardingRequest;
import com.industrial.mdm.modules.serviceProvider.dto.PublicProviderOnboardingResponse;
import com.industrial.mdm.modules.serviceProvider.dto.ServiceProviderApplicationResponse;
import com.industrial.mdm.modules.serviceProvider.dto.ServiceProviderProfileResponse;
import com.industrial.mdm.modules.serviceProvider.dto.ServiceProviderProfileUpdateRequest;
import com.industrial.mdm.modules.serviceProvider.repository.ProviderActivationTokenEntity;
import com.industrial.mdm.modules.serviceProvider.repository.ProviderActivationTokenRepository;
import com.industrial.mdm.modules.serviceProvider.repository.ServiceProviderApplicationEntity;
import com.industrial.mdm.modules.serviceProvider.repository.ServiceProviderApplicationRepository;
import com.industrial.mdm.modules.serviceProvider.repository.ServiceProviderEntity;
import com.industrial.mdm.modules.serviceProvider.repository.ServiceProviderProfileEntity;
import com.industrial.mdm.modules.serviceProvider.repository.ServiceProviderProfileRepository;
import com.industrial.mdm.modules.serviceProvider.repository.ServiceProviderRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceProviderService {

    private final AuthorizationService authorizationService;
    private final ServiceProviderApplicationRepository applicationRepository;
    private final ServiceProviderRepository providerRepository;
    private final ServiceProviderProfileRepository profileRepository;
    private final ProviderActivationTokenRepository activationTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailProvider emailProvider;
    private final String activationBaseUrl;

    public ServiceProviderService(
            AuthorizationService authorizationService,
            ServiceProviderApplicationRepository applicationRepository,
            ServiceProviderRepository providerRepository,
            ServiceProviderProfileRepository profileRepository,
            ProviderActivationTokenRepository activationTokenRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailProvider emailProvider,
            @Value("${mdm.auth.provider-activation-base-url:http://localhost:5273/auth/activate}")
                    String activationBaseUrl) {
        this.authorizationService = authorizationService;
        this.applicationRepository = applicationRepository;
        this.providerRepository = providerRepository;
        this.profileRepository = profileRepository;
        this.activationTokenRepository = activationTokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailProvider = emailProvider;
        this.activationBaseUrl = activationBaseUrl;
    }

    @Transactional
    public PublicProviderOnboardingResponse submitPublicOnboarding(PublicProviderOnboardingRequest request) {
        ServiceProviderApplicationEntity entity = new ServiceProviderApplicationEntity();
        entity.setCompanyName(normalizeRequired(request.companyName(), "provider company name is required"));
        entity.setContactName(normalizeRequired(request.contactName(), "provider contact name is required"));
        entity.setPhone(normalizeRequired(request.phone(), "provider phone is required"));
        entity.setEmail(normalizeEmail(request.email()));
        entity.setWebsite(normalizeOptional(request.website()));
        entity.setServiceScope(normalizeRequired(request.serviceScope(), "service scope is required"));
        entity.setSummary(normalizeRequired(request.summary(), "provider summary is required"));
        entity.setLogoUrl(normalizeOptional(request.logoUrl()));
        entity.setLicenseFileName(normalizeOptional(request.licenseFileName()));
        entity.setLicensePreviewUrl(normalizeOptional(request.licensePreviewUrl()));
        entity.setStatus(ServiceProviderApplicationStatus.PENDING_REVIEW);
        entity = applicationRepository.save(entity);
        return new PublicProviderOnboardingResponse(
                entity.getId(),
                entity.getStatus().getCode(),
                entity.getCompanyName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<ServiceProviderProfileResponse> listPublicProviders() {
        return providerRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(provider -> provider.getStatus() == ServiceProviderStatus.ACTIVE)
                .map(this::toProfileResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceProviderProfileResponse getPublicProvider(UUID providerId) {
        ServiceProviderEntity provider = loadProvider(providerId);
        if (provider.getStatus() != ServiceProviderStatus.ACTIVE) {
            throw new BizException(ErrorCode.NOT_FOUND, "service provider not found");
        }
        return toProfileResponse(provider);
    }

    @Transactional(readOnly = true)
    public List<ServiceProviderApplicationResponse> listProviderReviews(AuthenticatedUser currentUser) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.ADMIN_PROVIDER_REVIEW_LIST,
                "current account cannot read provider reviews");
        return applicationRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toApplicationResponse).toList();
    }

    @Transactional(readOnly = true)
    public ServiceProviderApplicationResponse getProviderReviewDetail(
            AuthenticatedUser currentUser, UUID applicationId) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.ADMIN_PROVIDER_REVIEW_DETAIL,
                "current account cannot read provider review detail");
        return toApplicationResponse(loadApplication(applicationId));
    }

    @Transactional(readOnly = true)
    public List<ServiceProviderProfileResponse> listAdminProviders(AuthenticatedUser currentUser) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.ADMIN_PROVIDER_LIST,
                "current account cannot read service providers");
        return providerRepository.findAllByOrderByUpdatedAtDesc().stream().map(this::toProfileResponse).toList();
    }

    @Transactional
    public ServiceProviderApplicationResponse approveProviderReview(
            AuthenticatedUser currentUser, UUID applicationId, ProviderReviewDecisionRequest request) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.ADMIN_PROVIDER_REVIEW_APPROVE,
                "current account cannot approve provider review");
        ServiceProviderApplicationEntity application = loadApplication(applicationId);
        ServiceProviderEntity provider = application.getApprovedProviderId() == null
                ? createProviderFromApplication(application)
                : loadProvider(application.getApprovedProviderId());
        application.setStatus(ServiceProviderApplicationStatus.APPROVED);
        application.setReviewComment(normalizeOptional(request.reviewComment()));
        application.setReviewedBy(currentUser.userId());
        application.setReviewedAt(OffsetDateTime.now());
        application.setApprovedProviderId(provider.getId());
        applicationRepository.save(application);

        provider.setLatestApplicationId(application.getId());
        provider.setLastReviewComment(application.getReviewComment());
        providerRepository.save(provider);
        issueActivationLinkIfNeeded(provider.getId());
        return toApplicationResponse(application);
    }

    @Transactional
    public ServiceProviderApplicationResponse rejectProviderReview(
            AuthenticatedUser currentUser, UUID applicationId, ProviderReviewDecisionRequest request) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.ADMIN_PROVIDER_REVIEW_REJECT,
                "current account cannot reject provider review");
        ServiceProviderApplicationEntity application = loadApplication(applicationId);
        application.setStatus(ServiceProviderApplicationStatus.REJECTED);
        application.setReviewComment(normalizeRequired(request.reviewComment(), "review comment is required"));
        application.setReviewedBy(currentUser.userId());
        application.setReviewedAt(OffsetDateTime.now());
        applicationRepository.save(application);
        return toApplicationResponse(application);
    }

    @Transactional
    public ServiceProviderApplicationResponse resendProviderActivationLink(
            AuthenticatedUser currentUser, UUID applicationId) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.ADMIN_PROVIDER_ACTIVATION_RESEND,
                "current account cannot resend provider activation link");
        ServiceProviderApplicationEntity application = loadApplication(applicationId);
        if (application.getApprovedProviderId() == null
                || application.getStatus() != ServiceProviderApplicationStatus.APPROVED) {
            throw new BizException(
                    ErrorCode.STATE_CONFLICT,
                    "provider activation can only be resent after the application is approved");
        }
        issueActivationLinkIfNeeded(application.getApprovedProviderId());
        return toApplicationResponse(application);
    }

    @Transactional(readOnly = true)
    public ServiceProviderProfileResponse getCurrentProviderProfile(AuthenticatedUser currentUser) {
        UUID providerId =
                authorizationService.assertCurrentProviderPermission(
                        currentUser,
                        PermissionCode.PROVIDER_PROFILE_READ,
                        "current account cannot read provider profile");
        return toProfileResponse(loadProvider(providerId));
    }

    @Transactional
    public ServiceProviderProfileResponse updateCurrentProviderProfile(
            AuthenticatedUser currentUser, ServiceProviderProfileUpdateRequest request) {
        UUID providerId =
                authorizationService.assertCurrentProviderPermission(
                        currentUser,
                        PermissionCode.PROVIDER_PROFILE_UPDATE,
                        "current account cannot update provider profile");
        ServiceProviderEntity provider = loadProvider(providerId);
        ServiceProviderProfileEntity currentProfile = loadProviderProfile(provider.getId());
        ServiceProviderProfileEntity nextProfile = new ServiceProviderProfileEntity();
        nextProfile.setServiceProviderId(provider.getId());
        nextProfile.setVersionNo(currentProfile.getVersionNo() + 1);
        nextProfile.setCompanyName(normalizeRequired(request.companyName(), "provider company name is required"));
        nextProfile.setShortName(normalizeOptional(request.shortName()));
        nextProfile.setServiceScope(normalizeRequired(request.serviceScope(), "provider service scope is required"));
        nextProfile.setSummary(normalizeRequired(request.summary(), "provider summary is required"));
        nextProfile.setWebsite(normalizeOptional(request.website()));
        nextProfile.setLogoUrl(normalizeOptional(request.logoUrl()));
        nextProfile.setLicenseFileName(normalizeOptional(request.licenseFileName()));
        nextProfile.setLicensePreviewUrl(normalizeOptional(request.licensePreviewUrl()));
        nextProfile.setContactName(normalizeRequired(request.contactName(), "provider contact name is required"));
        nextProfile.setContactPhone(normalizeRequired(request.contactPhone(), "provider contact phone is required"));
        nextProfile.setContactEmail(normalizeEmail(request.contactEmail()));
        nextProfile = profileRepository.save(nextProfile);
        provider.setCurrentProfileId(nextProfile.getId());
        provider.setWorkingProfileId(nextProfile.getId());
        provider.setName(nextProfile.getCompanyName());
        providerRepository.save(provider);
        return toProfileResponse(provider);
    }

    @Transactional(readOnly = true)
    public ProviderActivationTokenPreviewResponse previewActivation(String token) {
        ProviderActivationTokenEntity activationToken = loadActiveToken(token);
        ServiceProviderProfileEntity profile = loadProviderProfile(activationToken.getServiceProviderId());
        return new ProviderActivationTokenPreviewResponse(
                profile.getCompanyName(),
                profile.getContactName(),
                activationToken.getAccount(),
                activationToken.getPhone(),
                activationToken.getEmail(),
                activationToken.getExpiresAt());
    }

    @Transactional
    public ProviderActivationCompleteResponse completeActivation(
            String token, ProviderActivationCompleteRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "password confirmation mismatch");
        }
        ProviderActivationTokenEntity activationToken = loadActiveToken(token);
        ServiceProviderEntity provider = loadProvider(activationToken.getServiceProviderId());
        ServiceProviderProfileEntity profile = loadProviderProfile(provider.getId());
        if (userRepository.findFirstByServiceProviderIdAndRole(provider.getId(), UserRole.PROVIDER_OWNER).isPresent()) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "provider owner already activated");
        }
        ensureUniqueAccount(activationToken.getAccount(), activationToken.getPhone(), activationToken.getEmail());

        UserEntity user = new UserEntity();
        user.setAccount(activationToken.getAccount());
        user.setPhone(activationToken.getPhone());
        user.setEmail(activationToken.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.PROVIDER_OWNER);
        user.setStatus(AccountStatus.ACTIVE);
        user.setServiceProviderId(provider.getId());
        user.setDisplayName(profile.getContactName());
        user.setOrganization(profile.getCompanyName());
        userRepository.save(user);

        activationToken.setUsedAt(OffsetDateTime.now());
        activationTokenRepository.save(activationToken);
        provider.setStatus(ServiceProviderStatus.ACTIVE);
        provider.setJoinedAt(LocalDate.now());
        providerRepository.save(provider);
        return new ProviderActivationCompleteResponse("/auth/login", profile.getCompanyName(), activationToken.getAccount());
    }

    @Transactional(readOnly = true)
    public ProviderActivationPreviewResponse getActivationPreview(UUID providerId) {
        return activationTokenRepository.findTopByServiceProviderIdOrderByCreatedAtDesc(providerId)
                .map(token -> new ProviderActivationPreviewResponse(
                        token.getAccount(),
                        token.getEmail(),
                        token.getPhone(),
                        token.getUsedAt() == null ? buildActivationLink(token.getTokenValue()) : null,
                        token.getCreatedAt(),
                        token.getExpiresAt(),
                        token.getUsedAt()))
                .orElse(null);
    }

    @Transactional
    public ProviderActivationPreviewResponse issueActivationLinkIfNeeded(UUID providerId) {
        if (userRepository.findFirstByServiceProviderIdAndRole(providerId, UserRole.PROVIDER_OWNER).isPresent()) {
            return getActivationPreview(providerId);
        }
        ServiceProviderProfileEntity profile = loadProviderProfile(providerId);
        activationTokenRepository.findByServiceProviderIdAndUsedAtIsNull(providerId)
                .forEach(token -> token.setUsedAt(OffsetDateTime.now()));
        ProviderActivationTokenEntity token = new ProviderActivationTokenEntity();
        token.setServiceProviderId(providerId);
        token.setPhone(profile.getContactPhone());
        token.setEmail(profile.getContactEmail());
        token.setAccount(selectLockedAccount(profile));
        token.setTokenValue(generateTokenValue());
        token.setExpiresAt(OffsetDateTime.now().plusDays(7));
        token = activationTokenRepository.save(token);
        String activationLink = buildActivationLink(token.getTokenValue());
        emailProvider.sendActivationLink(
                token.getEmail(),
                "服务商账号激活",
                "您的服务商入驻申请已通过审核，请点击链接完成账号激活。",
                activationLink);
        return getActivationPreview(providerId);
    }

    public ServiceProviderEntity loadProvider(UUID providerId) {
        if (providerId == null) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "service provider id is required");
        }
        return providerRepository.findById(providerId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "service provider not found"));
    }

    public ServiceProviderProfileEntity loadProviderProfile(UUID providerId) {
        return profileRepository.findTopByServiceProviderIdOrderByVersionNoDesc(providerId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "service provider profile not found"));
    }

    private ServiceProviderApplicationEntity loadApplication(UUID applicationId) {
        if (applicationId == null) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "provider application id is required");
        }
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "provider application not found"));
    }

    private ProviderActivationTokenEntity loadActiveToken(String tokenValue) {
        ProviderActivationTokenEntity token = activationTokenRepository
                .findTopByTokenValueOrderByCreatedAtDesc(tokenValue)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "provider activation link not found"));
        if (!token.isActive()) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "provider activation link expired");
        }
        return token;
    }

    private ServiceProviderEntity createProviderFromApplication(ServiceProviderApplicationEntity application) {
        ServiceProviderEntity provider = new ServiceProviderEntity();
        provider.setName(application.getCompanyName());
        provider.setStatus(ServiceProviderStatus.PENDING_ACTIVATION);
        provider = providerRepository.save(provider);
        ServiceProviderProfileEntity profile = new ServiceProviderProfileEntity();
        profile.setServiceProviderId(provider.getId());
        profile.setVersionNo(1);
        profile.setCompanyName(application.getCompanyName());
        profile.setShortName(null);
        profile.setServiceScope(application.getServiceScope());
        profile.setSummary(application.getSummary());
        profile.setWebsite(application.getWebsite());
        profile.setLogoUrl(application.getLogoUrl());
        profile.setLicenseFileName(application.getLicenseFileName());
        profile.setLicensePreviewUrl(application.getLicensePreviewUrl());
        profile.setContactName(application.getContactName());
        profile.setContactPhone(application.getPhone());
        profile.setContactEmail(application.getEmail());
        profile = profileRepository.save(profile);
        provider.setCurrentProfileId(profile.getId());
        provider.setWorkingProfileId(profile.getId());
        provider.setLatestApplicationId(application.getId());
        return providerRepository.save(provider);
    }

    private ServiceProviderProfileResponse toProfileResponse(ServiceProviderEntity provider) {
        ServiceProviderProfileEntity profile = loadProviderProfile(provider.getId());
        return new ServiceProviderProfileResponse(
                provider.getId(),
                profile.getCompanyName(),
                profile.getShortName(),
                profile.getServiceScope(),
                profile.getSummary(),
                profile.getWebsite(),
                profile.getLogoUrl(),
                profile.getLicenseFileName(),
                profile.getLicensePreviewUrl(),
                profile.getContactName(),
                profile.getContactPhone(),
                profile.getContactEmail(),
                provider.getStatus().getCode(),
                provider.getJoinedAt(),
                provider.getLastReviewComment());
    }

    private ServiceProviderApplicationResponse toApplicationResponse(ServiceProviderApplicationEntity application) {
        return new ServiceProviderApplicationResponse(
                application.getId(),
                application.getCompanyName(),
                application.getContactName(),
                application.getPhone(),
                application.getEmail(),
                application.getWebsite(),
                application.getServiceScope(),
                application.getSummary(),
                application.getLogoUrl(),
                application.getLicenseFileName(),
                application.getLicensePreviewUrl(),
                application.getStatus().getCode(),
                application.getReviewComment(),
                application.getReviewedAt(),
                application.getCreatedAt(),
                application.getApprovedProviderId() == null ? null : getActivationPreview(application.getApprovedProviderId()));
    }

    private void ensureUniqueAccount(String account, String phone, String email) {
        if (userRepository.existsByAccountIgnoreCase(account)
                || userRepository.existsByPhone(phone)
                || userRepository.existsByEmailIgnoreCase(email)) {
            throw new BizException(ErrorCode.DUPLICATE_ACCOUNT, "account already exists");
        }
    }

    private String selectLockedAccount(ServiceProviderProfileEntity profile) {
        if (profile.getContactEmail() != null && !profile.getContactEmail().isBlank()) {
            return profile.getContactEmail().trim().toLowerCase();
        }
        if (profile.getContactPhone() != null && !profile.getContactPhone().isBlank()) {
            return profile.getContactPhone().trim();
        }
        throw new BizException(ErrorCode.INVALID_REQUEST, "provider account contact is incomplete");
    }

    private String buildActivationLink(String token) {
        return activationBaseUrl + "?providerToken=" + token;
    }

    private String generateTokenValue() {
        return UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BizException(ErrorCode.INVALID_REQUEST, message);
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeEmail(String value) {
        return normalizeRequired(value, "email is required").toLowerCase();
    }
}
