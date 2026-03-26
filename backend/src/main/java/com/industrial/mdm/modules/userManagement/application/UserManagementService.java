package com.industrial.mdm.modules.userManagement.application;

import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.common.security.UserRole;
import com.industrial.mdm.modules.auth.domain.AccountStatus;
import com.industrial.mdm.modules.auth.repository.UserEntity;
import com.industrial.mdm.modules.auth.repository.UserRepository;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseEntity;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseRepository;
import com.industrial.mdm.modules.iam.application.AuthorizationProfile;
import com.industrial.mdm.modules.iam.application.AuthorizationService;
import com.industrial.mdm.modules.iam.application.AuthorizationStateService;
import com.industrial.mdm.modules.iam.domain.permission.PermissionCode;
import com.industrial.mdm.modules.iam.repository.AccessGrantEntity;
import com.industrial.mdm.modules.iam.repository.AccessGrantRepository;
import com.industrial.mdm.modules.iam.repository.AccessGrantRequestEntity;
import com.industrial.mdm.modules.iam.repository.AccessGrantRequestRepository;
import com.industrial.mdm.modules.iam.repository.CapabilityCatalogEntity;
import com.industrial.mdm.modules.iam.repository.CapabilityCatalogRepository;
import com.industrial.mdm.modules.iam.repository.IamAuditLogEntity;
import com.industrial.mdm.modules.iam.repository.IamAuditLogRepository;
import com.industrial.mdm.modules.iam.repository.ReviewDomainAssignmentEntity;
import com.industrial.mdm.modules.iam.repository.ReviewDomainAssignmentRepository;
import com.industrial.mdm.modules.iam.repository.RoleTemplateEntity;
import com.industrial.mdm.modules.iam.repository.RoleTemplateRepository;
import com.industrial.mdm.modules.iam.repository.UserCapabilityBindingEntity;
import com.industrial.mdm.modules.iam.repository.UserCapabilityBindingRepository;
import com.industrial.mdm.modules.iam.repository.UserRoleBindingEntity;
import com.industrial.mdm.modules.iam.repository.UserRoleBindingRepository;
import com.industrial.mdm.modules.userManagement.dto.AdminUserCreateRequest;
import com.industrial.mdm.modules.userManagement.dto.AdminUserCredentialResponse;
import com.industrial.mdm.modules.userManagement.dto.AdminUserDetailResponse;
import com.industrial.mdm.modules.userManagement.dto.AdminUserListItemResponse;
import com.industrial.mdm.modules.userManagement.dto.AdminUserListResponse;
import com.industrial.mdm.modules.userManagement.dto.AdminUserOptionsResponse;
import com.industrial.mdm.modules.userManagement.dto.AdminUserStatusResponse;
import com.industrial.mdm.modules.userManagement.dto.AdminUserUpdateRequest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserManagementService {

    private static final String PRINCIPAL_TYPE_USER = "user";
    private static final String DEFAULT_PLATFORM_ORGANIZATION = "平台运营中心";
    private static final String PASSWORD_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    private final AuthorizationService authorizationService;
    private final AuthorizationStateService authorizationStateService;
    private final UserRepository userRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final RoleTemplateRepository roleTemplateRepository;
    private final CapabilityCatalogRepository capabilityCatalogRepository;
    private final UserRoleBindingRepository userRoleBindingRepository;
    private final UserCapabilityBindingRepository userCapabilityBindingRepository;
    private final ReviewDomainAssignmentRepository reviewDomainAssignmentRepository;
    private final AccessGrantRepository accessGrantRepository;
    private final AccessGrantRequestRepository accessGrantRequestRepository;
    private final IamAuditLogRepository iamAuditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public UserManagementService(
            AuthorizationService authorizationService,
            AuthorizationStateService authorizationStateService,
            UserRepository userRepository,
            EnterpriseRepository enterpriseRepository,
            RoleTemplateRepository roleTemplateRepository,
            CapabilityCatalogRepository capabilityCatalogRepository,
            UserRoleBindingRepository userRoleBindingRepository,
            UserCapabilityBindingRepository userCapabilityBindingRepository,
            ReviewDomainAssignmentRepository reviewDomainAssignmentRepository,
            AccessGrantRepository accessGrantRepository,
            AccessGrantRequestRepository accessGrantRequestRepository,
            IamAuditLogRepository iamAuditLogRepository,
            PasswordEncoder passwordEncoder) {
        this.authorizationService = authorizationService;
        this.authorizationStateService = authorizationStateService;
        this.userRepository = userRepository;
        this.enterpriseRepository = enterpriseRepository;
        this.roleTemplateRepository = roleTemplateRepository;
        this.capabilityCatalogRepository = capabilityCatalogRepository;
        this.userRoleBindingRepository = userRoleBindingRepository;
        this.userCapabilityBindingRepository = userCapabilityBindingRepository;
        this.reviewDomainAssignmentRepository = reviewDomainAssignmentRepository;
        this.accessGrantRepository = accessGrantRepository;
        this.accessGrantRequestRepository = accessGrantRequestRepository;
        this.iamAuditLogRepository = iamAuditLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public AdminUserListResponse listUsers(
            AuthenticatedUser currentUser,
            String keyword,
            String userType,
            String role,
            String status,
            UUID enterpriseId,
            int page,
            int pageSize) {
        authorizationService.assertPermission(
                currentUser, PermissionCode.USER_MANAGE_LIST, "not authorized to list users");

        Pageable pageable =
                PageRequest.of(
                        Math.max(page - 1, 0),
                        Math.min(Math.max(pageSize, 1), 100),
                        Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<UserEntity> userPage =
                userRepository.findAll(
                        buildUserSpecification(keyword, userType, role, status, enterpriseId), pageable);
        Map<UUID, EnterpriseEntity> enterpriseMap = loadEnterpriseMap(userPage.getContent());
        List<AdminUserListItemResponse> items =
                userPage.getContent().stream()
                        .map(user -> toListItem(user, enterpriseMap.get(user.getEnterpriseId())))
                        .toList();
        return new AdminUserListResponse(
                items, userPage.getTotalElements(), userPage.getNumber() + 1, userPage.getSize());
    }

    @Transactional(readOnly = true)
    public AdminUserOptionsResponse getOptions(AuthenticatedUser currentUser) {
        authorizationService.assertPermission(
                currentUser, PermissionCode.USER_MANAGE_LIST, "not authorized to read user options");

        List<AdminUserOptionsResponse.EnterpriseOption> enterprises =
                enterpriseRepository.findAllByOrderByNameAsc().stream()
                        .map(
                                enterprise ->
                                        new AdminUserOptionsResponse.EnterpriseOption(
                                                enterprise.getId(),
                                                enterprise.getName(),
                                                enterprise.getStatus().name().toLowerCase(Locale.ROOT)))
                        .toList();
        List<AdminUserOptionsResponse.RoleTemplateOption> roleTemplates =
                roleTemplateRepository.findAllByOrderByNameAsc().stream()
                        .map(
                                template ->
                                        new AdminUserOptionsResponse.RoleTemplateOption(
                                                template.getCode(),
                                                template.getName(),
                                                template.getLegacyRoleCode(),
                                                template.isBuiltIn()))
                        .toList();
        List<AdminUserOptionsResponse.CapabilityOption> capabilities =
                capabilityCatalogRepository.findAllByOrderByCodeAsc().stream()
                        .map(
                                capability ->
                                        new AdminUserOptionsResponse.CapabilityOption(
                                                capability.getCode(), capability.getDescription()))
                        .toList();
        return new AdminUserOptionsResponse(enterprises, roleTemplates, capabilities);
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse getDetail(AuthenticatedUser currentUser, UUID userId) {
        authorizationService.assertPermission(
                currentUser, PermissionCode.USER_MANAGE_DETAIL, "not authorized to read user detail");
        UserEntity user = loadUser(userId);
        return toDetail(user);
    }

    @Transactional
    public AdminUserCredentialResponse createUser(
            AuthenticatedUser currentUser, AdminUserCreateRequest request) {
        authorizationService.assertPermission(
                currentUser, PermissionCode.USER_MANAGE_CREATE, "not authorized to create user");

        UserRole role = parseRole(request.role());
        UUID enterpriseId = request.enterpriseId();
        EnterpriseEntity enterprise = resolveEnterpriseForCreate(role, enterpriseId);
        String account = normalizeRequired(request.account(), "account is required");
        String phone = normalizeRequired(request.phone(), "phone is required");
        String email = normalizeEmail(request.email());
        ensureUniqueAccount(account, phone, email, null);

        if (role == UserRole.ENTERPRISE_OWNER
                && userRepository.findFirstByEnterpriseIdAndRole(enterpriseId, UserRole.ENTERPRISE_OWNER)
                        .isPresent()) {
            throw new BizException(
                    ErrorCode.STATE_CONFLICT, "enterprise already has a primary owner account");
        }

        String rawPassword = normalizeOptional(request.password());
        if (rawPassword == null) {
            rawPassword = generateTemporaryPassword();
        }

        UserEntity user = new UserEntity();
        user.setAccount(account);
        user.setPhone(phone);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setStatus(AccountStatus.ACTIVE);
        user.setEnterpriseId(enterpriseId);
        user.setDisplayName(normalizeRequired(request.displayName(), "display name is required"));
        user.setOrganization(resolveOrganization(role, enterprise, request.organization()));
        user = userRepository.save(user);

        return new AdminUserCredentialResponse(
                user.getId(), user.getAccount(), rawPassword, OffsetDateTime.now());
    }

    @Transactional
    public AdminUserDetailResponse updateUser(
            AuthenticatedUser currentUser, UUID userId, AdminUserUpdateRequest request) {
        authorizationService.assertPermission(
                currentUser, PermissionCode.USER_MANAGE_UPDATE, "not authorized to update user");
        UserEntity user = loadUser(userId);
        EnterpriseEntity enterprise =
                user.getEnterpriseId() == null
                        ? null
                        : enterpriseRepository.findById(user.getEnterpriseId()).orElse(null);

        String account = normalizeRequired(request.account(), "account is required");
        String phone = normalizeRequired(request.phone(), "phone is required");
        String email = normalizeEmail(request.email());
        ensureUniqueAccount(account, phone, email, user.getId());

        user.setAccount(account);
        user.setPhone(phone);
        user.setEmail(email);
        user.setDisplayName(normalizeRequired(request.displayName(), "display name is required"));
        user.setOrganization(resolveOrganization(user.getRole(), enterprise, request.organization()));
        userRepository.save(user);
        authorizationStateService.invalidateUserAuthorization(user);

        return toDetail(user);
    }

    @Transactional
    public AdminUserStatusResponse enableUser(AuthenticatedUser currentUser, UUID userId) {
        authorizationService.assertPermission(
                currentUser, PermissionCode.USER_MANAGE_ENABLE, "not authorized to enable user");
        UserEntity user = loadUser(userId);
        user.setStatus(AccountStatus.ACTIVE);
        userRepository.save(user);
        authorizationStateService.invalidateUserAuthorization(user);
        return new AdminUserStatusResponse(user.getId(), user.getStatus().name().toLowerCase(Locale.ROOT));
    }

    @Transactional
    public AdminUserStatusResponse disableUser(AuthenticatedUser currentUser, UUID userId) {
        authorizationService.assertPermission(
                currentUser, PermissionCode.USER_MANAGE_DISABLE, "not authorized to disable user");
        UserEntity user = loadUser(userId);
        user.setStatus(AccountStatus.FROZEN);
        userRepository.save(user);
        authorizationStateService.invalidateUserAuthorization(user);
        return new AdminUserStatusResponse(user.getId(), user.getStatus().name().toLowerCase(Locale.ROOT));
    }

    @Transactional
    public AdminUserCredentialResponse resetPassword(AuthenticatedUser currentUser, UUID userId) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.USER_MANAGE_RESET_PASSWORD,
                "not authorized to reset password");
        UserEntity user = loadUser(userId);
        String temporaryPassword = generateTemporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        userRepository.save(user);
        authorizationStateService.invalidateUserAuthorization(user);
        return new AdminUserCredentialResponse(
                user.getId(), user.getAccount(), temporaryPassword, OffsetDateTime.now());
    }

    private Specification<UserEntity> buildUserSpecification(
            String keyword,
            String userType,
            String role,
            String status,
            UUID enterpriseId) {
        return (root, query, criteriaBuilder) -> {
            var predicate = criteriaBuilder.conjunction();

            String normalizedKeyword = normalizeOptional(keyword);
            if (normalizedKeyword != null) {
                String pattern = "%" + normalizedKeyword.toLowerCase(Locale.ROOT) + "%";
                predicate =
                        criteriaBuilder.and(
                                predicate,
                                criteriaBuilder.or(
                                        criteriaBuilder.like(
                                                criteriaBuilder.lower(root.get("displayName")), pattern),
                                        criteriaBuilder.like(
                                                criteriaBuilder.lower(root.get("account")), pattern),
                                        criteriaBuilder.like(
                                                criteriaBuilder.lower(root.get("phone")), pattern),
                                        criteriaBuilder.like(
                                                criteriaBuilder.lower(root.get("email")), pattern),
                                        criteriaBuilder.like(
                                                criteriaBuilder.lower(root.get("organization")), pattern)));
            }

            String normalizedUserType = normalizeOptional(userType);
            if ("platform".equalsIgnoreCase(normalizedUserType)) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.isNull(root.get("enterpriseId")));
            } else if ("enterprise".equalsIgnoreCase(normalizedUserType)) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.isNotNull(root.get("enterpriseId")));
            }

            String normalizedRole = normalizeOptional(role);
            if (normalizedRole != null) {
                predicate =
                        criteriaBuilder.and(
                                predicate,
                                criteriaBuilder.equal(root.get("role"), parseRole(normalizedRole)));
            }

            String normalizedStatus = normalizeOptional(status);
            if (normalizedStatus != null) {
                predicate =
                        criteriaBuilder.and(
                                predicate,
                                criteriaBuilder.equal(root.get("status"), parseStatus(normalizedStatus)));
            }

            if (enterpriseId != null) {
                predicate =
                        criteriaBuilder.and(
                                predicate, criteriaBuilder.equal(root.get("enterpriseId"), enterpriseId));
            }

            return predicate;
        };
    }

    private AdminUserDetailResponse toDetail(UserEntity user) {
        Map<UUID, EnterpriseEntity> enterpriseMap = loadEnterpriseMap(List.of(user));
        List<UserRoleBindingEntity> roleBindings =
                userRoleBindingRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        List<UserCapabilityBindingEntity> capabilityBindings =
                userCapabilityBindingRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        List<ReviewDomainAssignmentEntity> reviewDomainAssignments =
                reviewDomainAssignmentRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        List<AccessGrantEntity> accessGrants =
                accessGrantRepository.findByPrincipalTypeAndPrincipalIdOrderByCreatedAtDesc(
                        PRINCIPAL_TYPE_USER, user.getId());
        List<AccessGrantRequestEntity> accessGrantRequests =
                accessGrantRequestRepository.findTop20ByTargetUserIdOrderByCreatedAtDesc(user.getId());
        List<IamAuditLogEntity> auditLogs =
                iamAuditLogRepository.findTop20ByTargetUserIdOrderByCreatedAtDesc(user.getId());

        Set<UUID> referencedEnterpriseIds = new HashSet<>();
        if (user.getEnterpriseId() != null) {
            referencedEnterpriseIds.add(user.getEnterpriseId());
        }
        roleBindings.stream()
                .map(UserRoleBindingEntity::getEnterpriseId)
                .filter(Objects::nonNull)
                .forEach(referencedEnterpriseIds::add);
        reviewDomainAssignments.stream()
                .map(ReviewDomainAssignmentEntity::getEnterpriseId)
                .filter(Objects::nonNull)
                .forEach(referencedEnterpriseIds::add);
        accessGrants.stream()
                .map(AccessGrantEntity::getEnterpriseId)
                .filter(Objects::nonNull)
                .forEach(referencedEnterpriseIds::add);
        accessGrantRequests.stream()
                .map(AccessGrantRequestEntity::getEnterpriseId)
                .filter(Objects::nonNull)
                .forEach(referencedEnterpriseIds::add);
        if (!referencedEnterpriseIds.isEmpty()) {
            enterpriseMap.putAll(
                    enterpriseRepository.findAllById(referencedEnterpriseIds).stream()
                            .collect(Collectors.toMap(EnterpriseEntity::getId, Function.identity())));
        }

        Map<UUID, RoleTemplateEntity> roleTemplateMap =
                roleTemplateRepository.findAll().stream()
                        .collect(Collectors.toMap(RoleTemplateEntity::getId, Function.identity()));
        Map<UUID, CapabilityCatalogEntity> capabilityMap =
                capabilityCatalogRepository.findAll().stream()
                        .collect(Collectors.toMap(CapabilityCatalogEntity::getId, Function.identity()));

        AuthenticatedUser principal = toPrincipal(user);
        AuthorizationProfile profile = authorizationService.getProfile(principal);
        EnterpriseEntity enterprise = enterpriseMap.get(user.getEnterpriseId());

        return new AdminUserDetailResponse(
                new AdminUserDetailResponse.Summary(
                        user.getId(),
                        resolveUserType(user),
                        user.getDisplayName(),
                        user.getAccount(),
                        user.getPhone(),
                        user.getEmail(),
                        user.getRole().getCode(),
                        user.getStatus().name().toLowerCase(Locale.ROOT),
                        user.getEnterpriseId(),
                        enterprise == null ? null : enterprise.getName(),
                        user.getOrganization(),
                        user.getLastLoginAt(),
                        user.getCreatedAt(),
                        user.getUpdatedAt()),
                new AdminUserDetailResponse.EffectiveAuthorization(
                        profile.permissions().stream()
                                .map(permission -> permission.getCode())
                                .sorted()
                                .toList(),
                        profile.dataScopes().stream()
                                .map(dataScope -> dataScope.getCode())
                                .sorted()
                                .toList(),
                        profile.capabilities().stream()
                                .map(capability -> capability.getCode())
                                .sorted()
                                .toList()),
                roleBindings.stream()
                        .map(binding -> toRoleBindingItem(binding, roleTemplateMap, enterpriseMap))
                        .toList(),
                capabilityBindings.stream()
                        .map(binding -> toCapabilityBindingItem(binding, capabilityMap))
                        .toList(),
                reviewDomainAssignments.stream()
                        .map(binding -> toReviewDomainItem(binding, enterpriseMap))
                        .toList(),
                accessGrants.stream()
                        .map(binding -> toAccessGrantItem(binding, enterpriseMap))
                        .toList(),
                accessGrantRequests.stream()
                        .map(binding -> toAccessGrantRequestItem(binding, enterpriseMap))
                        .toList(),
                auditLogs.stream()
                        .sorted(Comparator.comparing(IamAuditLogEntity::getCreatedAt).reversed())
                        .map(this::toAuditLogItem)
                        .toList());
    }

    private AdminUserDetailResponse.RoleBindingItem toRoleBindingItem(
            UserRoleBindingEntity binding,
            Map<UUID, RoleTemplateEntity> roleTemplateMap,
            Map<UUID, EnterpriseEntity> enterpriseMap) {
        RoleTemplateEntity roleTemplate = roleTemplateMap.get(binding.getRoleTemplateId());
        EnterpriseEntity enterprise = enterpriseMap.get(binding.getEnterpriseId());
        return new AdminUserDetailResponse.RoleBindingItem(
                binding.getId(),
                roleTemplate == null ? null : roleTemplate.getCode(),
                roleTemplate == null ? null : roleTemplate.getName(),
                binding.getSourceType(),
                binding.getEnterpriseId(),
                enterprise == null ? null : enterprise.getName(),
                binding.getEffectiveFrom(),
                binding.getExpiresAt(),
                binding.getRevokedAt(),
                binding.getReason());
    }

    private AdminUserDetailResponse.CapabilityBindingItem toCapabilityBindingItem(
            UserCapabilityBindingEntity binding,
            Map<UUID, CapabilityCatalogEntity> capabilityMap) {
        CapabilityCatalogEntity capability = capabilityMap.get(binding.getCapabilityId());
        return new AdminUserDetailResponse.CapabilityBindingItem(
                binding.getId(),
                capability == null ? null : capability.getCode(),
                capability == null ? null : capability.getDescription(),
                binding.getSourceType(),
                binding.getEffectiveFrom(),
                binding.getExpiresAt(),
                binding.getRevokedAt(),
                binding.getReason());
    }

    private AdminUserDetailResponse.ReviewDomainAssignmentItem toReviewDomainItem(
            ReviewDomainAssignmentEntity binding, Map<UUID, EnterpriseEntity> enterpriseMap) {
        EnterpriseEntity enterprise = enterpriseMap.get(binding.getEnterpriseId());
        return new AdminUserDetailResponse.ReviewDomainAssignmentItem(
                binding.getId(),
                binding.getDomainType(),
                binding.getEnterpriseId(),
                enterprise == null ? null : enterprise.getName(),
                binding.getEffectiveFrom(),
                binding.getExpiresAt(),
                binding.getRevokedAt(),
                binding.getReason());
    }

    private AdminUserDetailResponse.AccessGrantItem toAccessGrantItem(
            AccessGrantEntity grant, Map<UUID, EnterpriseEntity> enterpriseMap) {
        EnterpriseEntity enterprise = enterpriseMap.get(grant.getEnterpriseId());
        return new AdminUserDetailResponse.AccessGrantItem(
                grant.getId(),
                grant.getPermissionCode(),
                grant.getEnterpriseId(),
                enterprise == null ? null : enterprise.getName(),
                grant.getScopeType(),
                grant.getScopeValue(),
                grant.getResourceType(),
                grant.getResourceId(),
                grant.getGrantType(),
                grant.getEffect(),
                grant.getEffectiveFrom(),
                grant.getExpiresAt(),
                grant.getRevokedAt(),
                grant.getReason(),
                grant.getTicketNo());
    }

    private AdminUserDetailResponse.AccessGrantRequestItem toAccessGrantRequestItem(
            AccessGrantRequestEntity request, Map<UUID, EnterpriseEntity> enterpriseMap) {
        EnterpriseEntity enterprise = enterpriseMap.get(request.getEnterpriseId());
        return new AdminUserDetailResponse.AccessGrantRequestItem(
                request.getId(),
                request.getPermissionCode(),
                request.getEnterpriseId(),
                enterprise == null ? null : enterprise.getName(),
                request.getStatus(),
                request.getEffectiveFrom(),
                request.getExpiresAt(),
                request.getCreatedAt(),
                request.getReason(),
                request.getTicketNo(),
                request.getDecisionComment());
    }

    private AdminUserDetailResponse.AuditLogItem toAuditLogItem(IamAuditLogEntity log) {
        return new AdminUserDetailResponse.AuditLogItem(
                log.getId(),
                log.getActionCode(),
                log.getSummary(),
                log.getDetailJson(),
                log.getCreatedAt());
    }

    private AdminUserListItemResponse toListItem(UserEntity user, EnterpriseEntity enterprise) {
        return new AdminUserListItemResponse(
                user.getId(),
                resolveUserType(user),
                user.getDisplayName(),
                user.getAccount(),
                user.getPhone(),
                user.getEmail(),
                user.getRole().getCode(),
                user.getStatus().name().toLowerCase(Locale.ROOT),
                user.getEnterpriseId(),
                enterprise == null ? null : enterprise.getName(),
                user.getOrganization(),
                user.getLastLoginAt(),
                user.getCreatedAt());
    }

    private Map<UUID, EnterpriseEntity> loadEnterpriseMap(List<UserEntity> users) {
        Set<UUID> enterpriseIds =
                users.stream()
                        .map(UserEntity::getEnterpriseId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        if (enterpriseIds.isEmpty()) {
            return new java.util.HashMap<>();
        }
        return enterpriseRepository.findAllById(enterpriseIds).stream()
                .collect(Collectors.toMap(EnterpriseEntity::getId, Function.identity()));
    }

    private UserEntity loadUser(UUID userId) {
        if (userId == null) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "userId is required");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "user not found"));
    }

    private EnterpriseEntity resolveEnterpriseForCreate(UserRole role, UUID enterpriseId) {
        if (role == UserRole.ENTERPRISE_OWNER) {
            if (enterpriseId == null) {
                throw new BizException(
                        ErrorCode.INVALID_REQUEST, "enterpriseId is required for enterprise owner");
            }
            return enterpriseRepository.findById(enterpriseId)
                    .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "enterprise not found"));
        }
        if (enterpriseId != null) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "platform user cannot bind an enterprise");
        }
        return null;
    }

    private void ensureUniqueAccount(String account, String phone, String email, UUID currentUserId) {
        if (currentUserId == null) {
            if (userRepository.existsByAccountIgnoreCase(account)
                    || userRepository.existsByPhone(phone)
                    || userRepository.existsByEmailIgnoreCase(email)) {
                throw new BizException(ErrorCode.DUPLICATE_ACCOUNT, "account, phone or email already exists");
            }
            return;
        }

        if (userRepository.existsByAccountIgnoreCaseAndIdNot(account, currentUserId)
                || userRepository.existsByPhoneAndIdNot(phone, currentUserId)
                || userRepository.existsByEmailIgnoreCaseAndIdNot(email, currentUserId)) {
            throw new BizException(ErrorCode.DUPLICATE_ACCOUNT, "account, phone or email already exists");
        }
    }

    private AuthenticatedUser toPrincipal(UserEntity user) {
        return new AuthenticatedUser(
                user.getId(),
                user.getRole(),
                user.getEnterpriseId(),
                user.getServiceProviderId(),
                user.getDisplayName(),
                user.getOrganization(),
                user.getAuthzVersion() == null ? 0 : user.getAuthzVersion());
    }

    private String resolveOrganization(UserRole role, EnterpriseEntity enterprise, String organization) {
        if (role == UserRole.ENTERPRISE_OWNER) {
            return enterprise == null ? DEFAULT_PLATFORM_ORGANIZATION : enterprise.getName();
        }
        String normalized = normalizeOptional(organization);
        return normalized == null ? DEFAULT_PLATFORM_ORGANIZATION : normalized;
    }

    private UserRole parseRole(String role) {
        try {
            return UserRole.fromCode(normalizeRequired(role, "role is required"));
        } catch (IllegalArgumentException exception) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "unsupported role");
        }
    }

    private AccountStatus parseStatus(String status) {
        try {
            return AccountStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "unsupported account status");
        }
    }

    private String resolveUserType(UserEntity user) {
        return user.getEnterpriseId() == null ? "platform" : "enterprise";
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new BizException(ErrorCode.INVALID_REQUEST, message);
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        return normalizeRequired(email, "email is required").toLowerCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String generateTemporaryPassword() {
        StringBuilder builder = new StringBuilder(12);
        for (int index = 0; index < 12; index++) {
            int randomIndex = secureRandom.nextInt(PASSWORD_ALPHABET.length());
            builder.append(PASSWORD_ALPHABET.charAt(randomIndex));
        }
        return builder.toString();
    }
}
