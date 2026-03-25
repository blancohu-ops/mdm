package com.industrial.mdm.modules.iam.application;

import com.industrial.mdm.common.exception.BizException;
import com.industrial.mdm.common.exception.ErrorCode;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.common.security.UserRole;
import com.industrial.mdm.modules.auth.repository.UserEntity;
import com.industrial.mdm.modules.auth.repository.UserRepository;
import com.industrial.mdm.modules.iam.domain.audit.IamAuditAction;
import com.industrial.mdm.modules.iam.domain.permission.PermissionCode;
import com.industrial.mdm.modules.iam.domain.scope.DataScopeCode;
import com.industrial.mdm.modules.iam.repository.AccessGrantEntity;
import com.industrial.mdm.modules.iam.repository.AccessGrantRepository;
import com.industrial.mdm.modules.iam.repository.CapabilityCatalogEntity;
import com.industrial.mdm.modules.iam.repository.CapabilityCatalogRepository;
import com.industrial.mdm.modules.iam.repository.IamAuditLogEntity;
import com.industrial.mdm.modules.iam.repository.RoleTemplateEntity;
import com.industrial.mdm.modules.iam.repository.RoleTemplateRepository;
import com.industrial.mdm.modules.iam.repository.UserCapabilityBindingEntity;
import com.industrial.mdm.modules.iam.repository.UserCapabilityBindingRepository;
import com.industrial.mdm.modules.iam.repository.UserRoleBindingEntity;
import com.industrial.mdm.modules.iam.repository.UserRoleBindingRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthorizationAdministrationService {

    static final String SOURCE_TYPE_MANUAL_ASSIGNMENT = "manual_assignment";
    static final String GRANT_TYPE_TEMPORARY_ACCESS = "temporary_access";
    static final String TARGET_TYPE_ROLE_BINDING = "role_binding";
    static final String TARGET_TYPE_CAPABILITY_BINDING = "capability_binding";
    static final String TARGET_TYPE_ACCESS_GRANT = "access_grant";
    static final String PRINCIPAL_TYPE_USER = "user";
    static final String EFFECT_ALLOW = "allow";

    private final AuthorizationService authorizationService;
    private final AuthorizationStateService authorizationStateService;
    private final IamAuditLogService iamAuditLogService;
    private final UserRepository userRepository;
    private final RoleTemplateRepository roleTemplateRepository;
    private final CapabilityCatalogRepository capabilityCatalogRepository;
    private final UserRoleBindingRepository userRoleBindingRepository;
    private final UserCapabilityBindingRepository userCapabilityBindingRepository;
    private final AccessGrantRepository accessGrantRepository;

    public AuthorizationAdministrationService(
            AuthorizationService authorizationService,
            AuthorizationStateService authorizationStateService,
            IamAuditLogService iamAuditLogService,
            UserRepository userRepository,
            RoleTemplateRepository roleTemplateRepository,
            CapabilityCatalogRepository capabilityCatalogRepository,
            UserRoleBindingRepository userRoleBindingRepository,
            UserCapabilityBindingRepository userCapabilityBindingRepository,
            AccessGrantRepository accessGrantRepository) {
        this.authorizationService = authorizationService;
        this.authorizationStateService = authorizationStateService;
        this.iamAuditLogService = iamAuditLogService;
        this.userRepository = userRepository;
        this.roleTemplateRepository = roleTemplateRepository;
        this.capabilityCatalogRepository = capabilityCatalogRepository;
        this.userRoleBindingRepository = userRoleBindingRepository;
        this.userCapabilityBindingRepository = userCapabilityBindingRepository;
        this.accessGrantRepository = accessGrantRepository;
    }

    @Transactional
    public AuthorizationMutationResult grantRoleTemplate(
            AuthenticatedUser currentUser, GrantRoleTemplateCommand command) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.ROLE_TEMPLATE_GRANT,
                "not authorized to grant role templates");
        validateReason(command.reason());
        Range range = normalizeRange(command.effectiveFrom(), command.expiresAt());
        UserEntity targetUser = loadTargetUser(command.targetUserId());
        RoleTemplateEntity roleTemplate = loadRoleTemplate(command.roleTemplateCode());
        UUID scopedEnterpriseId = resolveRoleBindingEnterpriseId(targetUser, roleTemplate);
        OffsetDateTime now = OffsetDateTime.now();

        boolean duplicateExists =
                userRoleBindingRepository
                        .findByUserIdAndRevokedAtIsNullAndEffectiveFromLessThanEqual(
                                targetUser.getId(), now)
                        .stream()
                        .filter(binding -> isActiveAt(binding.getExpiresAt(), now))
                        .anyMatch(
                                binding ->
                                        binding.getRoleTemplateId().equals(roleTemplate.getId())
                                                && Objects.equals(
                                                        binding.getEnterpriseId(), scopedEnterpriseId)
                                                && SOURCE_TYPE_MANUAL_ASSIGNMENT.equals(
                                                        binding.getSourceType()));
        if (duplicateExists) {
            throw new BizException(
                    ErrorCode.STATE_CONFLICT, "matching role template binding already exists");
        }

        UserRoleBindingEntity binding = new UserRoleBindingEntity();
        binding.setUserId(targetUser.getId());
        binding.setRoleTemplateId(roleTemplate.getId());
        binding.setEnterpriseId(scopedEnterpriseId);
        binding.setSourceType(SOURCE_TYPE_MANUAL_ASSIGNMENT);
        binding.setPrimary(false);
        binding.setGrantedBy(currentUser.userId());
        binding.setReason(command.reason().trim());
        binding.setEffectiveFrom(range.effectiveFrom());
        binding.setExpiresAt(range.expiresAt());
        binding = userRoleBindingRepository.save(binding);

        authorizationStateService.invalidateUserAuthorization(targetUser);
        iamAuditLogService.record(
                currentUser,
                IamAuditAction.ROLE_BINDING_GRANTED,
                TARGET_TYPE_ROLE_BINDING,
                binding.getId(),
                targetUser.getId(),
                scopedEnterpriseId,
                "Granted role template " + roleTemplate.getCode(),
                Map.of(
                        "roleTemplateCode", roleTemplate.getCode(),
                        "sourceType", binding.getSourceType(),
                        "reason", binding.getReason()));
        return AuthorizationMutationResult.fromRoleBinding(binding, roleTemplate.getCode());
    }

    @Transactional
    public AuthorizationMutationResult revokeRoleTemplateBinding(
            AuthenticatedUser currentUser, UUID bindingId, String reason) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.ROLE_TEMPLATE_GRANT,
                "not authorized to revoke role templates");
        validateReason(reason);
        UserRoleBindingEntity binding =
                userRoleBindingRepository
                        .findById(bindingId)
                        .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "role binding not found"));
        if (binding.getRevokedAt() != null) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "role binding already revoked");
        }
        binding.setRevokedAt(OffsetDateTime.now());
        binding.setRevokedBy(currentUser.userId());
        binding.setRevokedReason(reason.trim());
        userRoleBindingRepository.save(binding);

        UserEntity targetUser = loadTargetUser(binding.getUserId());
        authorizationStateService.invalidateUserAuthorization(targetUser);
        iamAuditLogService.record(
                currentUser,
                IamAuditAction.ROLE_BINDING_REVOKED,
                TARGET_TYPE_ROLE_BINDING,
                binding.getId(),
                binding.getUserId(),
                binding.getEnterpriseId(),
                "Revoked role binding",
                Map.of("reason", binding.getRevokedReason(), "sourceType", binding.getSourceType()));
        return AuthorizationMutationResult.fromRoleBinding(binding, "revoked");
    }

    @Transactional
    public AuthorizationMutationResult grantCapabilityBinding(
            AuthenticatedUser currentUser, GrantCapabilityBindingCommand command) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.CAPABILITY_BINDING_GRANT,
                "not authorized to grant capability bindings");
        validateReason(command.reason());
        Range range = normalizeRange(command.effectiveFrom(), command.expiresAt());
        UserEntity targetUser = loadTargetUser(command.targetUserId());
        CapabilityCatalogEntity capability = loadCapability(command.capabilityCode());
        OffsetDateTime now = OffsetDateTime.now();

        boolean duplicateExists =
                userCapabilityBindingRepository
                        .findByUserIdAndRevokedAtIsNullAndEffectiveFromLessThanEqual(
                                targetUser.getId(), now)
                        .stream()
                        .filter(binding -> isActiveAt(binding.getExpiresAt(), now))
                        .anyMatch(binding -> binding.getCapabilityId().equals(capability.getId()));
        if (duplicateExists) {
            throw new BizException(
                    ErrorCode.STATE_CONFLICT, "matching capability binding already exists");
        }

        UserCapabilityBindingEntity binding = new UserCapabilityBindingEntity();
        binding.setUserId(targetUser.getId());
        binding.setCapabilityId(capability.getId());
        binding.setSourceType(SOURCE_TYPE_MANUAL_ASSIGNMENT);
        binding.setGrantedBy(currentUser.userId());
        binding.setApprovedBy(currentUser.userId());
        binding.setReason(command.reason().trim());
        binding.setEffectiveFrom(range.effectiveFrom());
        binding.setExpiresAt(range.expiresAt());
        binding = userCapabilityBindingRepository.save(binding);

        authorizationStateService.invalidateUserAuthorization(targetUser);
        iamAuditLogService.record(
                currentUser,
                IamAuditAction.CAPABILITY_BINDING_GRANTED,
                TARGET_TYPE_CAPABILITY_BINDING,
                binding.getId(),
                targetUser.getId(),
                targetUser.getEnterpriseId(),
                "Granted capability " + capability.getCode(),
                Map.of("capabilityCode", capability.getCode(), "reason", binding.getReason()));
        return AuthorizationMutationResult.fromCapabilityBinding(binding, capability.getCode());
    }

    @Transactional
    public AuthorizationMutationResult revokeCapabilityBinding(
            AuthenticatedUser currentUser, UUID bindingId, String reason) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.CAPABILITY_BINDING_GRANT,
                "not authorized to revoke capability bindings");
        validateReason(reason);
        UserCapabilityBindingEntity binding =
                userCapabilityBindingRepository
                        .findById(bindingId)
                        .orElseThrow(
                                () ->
                                        new BizException(
                                                ErrorCode.NOT_FOUND, "capability binding not found"));
        if (binding.getRevokedAt() != null) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "capability binding already revoked");
        }
        binding.setRevokedAt(OffsetDateTime.now());
        userCapabilityBindingRepository.save(binding);

        UserEntity targetUser = loadTargetUser(binding.getUserId());
        authorizationStateService.invalidateUserAuthorization(targetUser);
        iamAuditLogService.record(
                currentUser,
                IamAuditAction.CAPABILITY_BINDING_REVOKED,
                TARGET_TYPE_CAPABILITY_BINDING,
                binding.getId(),
                binding.getUserId(),
                targetUser.getEnterpriseId(),
                "Revoked capability binding",
                Map.of("reason", reason.trim()));
        return AuthorizationMutationResult.fromCapabilityBinding(binding, "revoked");
    }

    @Transactional
    public AuthorizationMutationResult grantTemporaryAccess(
            AuthenticatedUser currentUser, GrantTemporaryAccessCommand command) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.ACCESS_GRANT_MANAGE,
                "not authorized to grant temporary access");
        return grantTemporaryAccessInternal(currentUser, command);
    }

    @Transactional
    public AuthorizationMutationResult grantApprovedTemporaryAccess(
            AuthenticatedUser currentUser, GrantTemporaryAccessCommand command) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.ACCESS_GRANT_REQUEST_APPROVE,
                "not authorized to materialize approved temporary access");
        return grantTemporaryAccessInternal(currentUser, command);
    }

    private AuthorizationMutationResult grantTemporaryAccessInternal(
            AuthenticatedUser currentUser, GrantTemporaryAccessCommand command) {
        validateReason(command.reason());
        Range range = normalizeRange(command.effectiveFrom(), command.expiresAt());
        UserEntity targetUser = loadTargetUser(command.targetUserId());
        PermissionCode permissionCode = parsePermission(command.permissionCode());
        DataScopeCode scopeCode = parseDataScope(command.scopeType());
        validateTemporaryAccessScope(targetUser, command, range);
        OffsetDateTime now = OffsetDateTime.now();

        boolean duplicateExists =
                accessGrantRepository
                        .findActiveGrants(PRINCIPAL_TYPE_USER, targetUser.getId(), now)
                        .stream()
                        .anyMatch(
                                grant ->
                                        permissionCode.getCode().equals(grant.getPermissionCode())
                                                && Objects.equals(command.enterpriseId(), grant.getEnterpriseId())
                                                && Objects.equals(
                                                        scopeCode == null ? null : scopeCode.getCode(),
                                                        grant.getScopeType())
                                                && Objects.equals(command.scopeValue(), grant.getScopeValue())
                                                && Objects.equals(command.resourceType(), grant.getResourceType())
                                                && Objects.equals(command.resourceId(), grant.getResourceId()));
        if (duplicateExists) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "matching temporary access already exists");
        }

        AccessGrantEntity grant = new AccessGrantEntity();
        grant.setPrincipalType(PRINCIPAL_TYPE_USER);
        grant.setPrincipalId(targetUser.getId());
        grant.setPermissionCode(permissionCode.getCode());
        grant.setEnterpriseId(command.enterpriseId());
        grant.setScopeType(scopeCode == null ? null : scopeCode.getCode());
        grant.setScopeValue(command.scopeValue());
        grant.setResourceType(command.resourceType());
        grant.setResourceId(command.resourceId());
        grant.setGrantType(GRANT_TYPE_TEMPORARY_ACCESS);
        grant.setEffect(EFFECT_ALLOW);
        grant.setGrantedBy(currentUser.userId());
        grant.setApprovedBy(currentUser.userId());
        grant.setReason(command.reason().trim());
        grant.setTicketNo(normalizeOptional(command.ticketNo()));
        grant.setEffectiveFrom(range.effectiveFrom());
        grant.setExpiresAt(range.expiresAt());
        grant = accessGrantRepository.save(grant);

        authorizationStateService.invalidateUserAuthorization(targetUser);
        iamAuditLogService.record(
                currentUser,
                IamAuditAction.ACCESS_GRANT_GRANTED,
                TARGET_TYPE_ACCESS_GRANT,
                grant.getId(),
                targetUser.getId(),
                command.enterpriseId(),
                "Granted temporary access " + permissionCode.getCode(),
                Map.of(
                        "permissionCode", permissionCode.getCode(),
                        "scopeType", grant.getScopeType() == null ? "" : grant.getScopeType(),
                        "resourceType", grant.getResourceType() == null ? "" : grant.getResourceType(),
                        "ticketNo", grant.getTicketNo() == null ? "" : grant.getTicketNo()));
        return AuthorizationMutationResult.fromAccessGrant(grant, permissionCode.getCode());
    }

    @Transactional
    public AuthorizationMutationResult revokeTemporaryAccess(
            AuthenticatedUser currentUser, UUID grantId, String reason) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.ACCESS_GRANT_MANAGE,
                "not authorized to revoke temporary access");
        validateReason(reason);
        AccessGrantEntity grant =
                accessGrantRepository
                        .findById(grantId)
                        .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "access grant not found"));
        if (grant.getRevokedAt() != null) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "access grant already revoked");
        }
        grant.setRevokedAt(OffsetDateTime.now());
        grant.setRevokedBy(currentUser.userId());
        grant.setRevokedReason(reason.trim());
        accessGrantRepository.save(grant);

        authorizationStateService.invalidateUserAuthorization(grant.getPrincipalId());
        iamAuditLogService.record(
                currentUser,
                IamAuditAction.ACCESS_GRANT_REVOKED,
                TARGET_TYPE_ACCESS_GRANT,
                grant.getId(),
                grant.getPrincipalId(),
                grant.getEnterpriseId(),
                "Revoked temporary access",
                Map.of("reason", grant.getRevokedReason(), "permissionCode", grant.getPermissionCode()));
        return AuthorizationMutationResult.fromAccessGrant(grant, grant.getPermissionCode());
    }

    @Transactional(readOnly = true)
    public List<IamAuditLogEntity> listRecentAuditLogs(AuthenticatedUser currentUser) {
        authorizationService.assertPermission(
                currentUser,
                PermissionCode.AUDIT_LOG_READ,
                "not authorized to read audit logs");
        return iamAuditLogService.listRecent();
    }

    private UserEntity loadTargetUser(UUID userId) {
        if (userId == null) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "target user is required");
        }
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "target user not found"));
    }

    private RoleTemplateEntity loadRoleTemplate(String roleTemplateCode) {
        if (roleTemplateCode == null || roleTemplateCode.isBlank()) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "role template code is required");
        }
        return roleTemplateRepository
                .findByCode(roleTemplateCode.trim())
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "role template not found"));
    }

    private CapabilityCatalogEntity loadCapability(String capabilityCode) {
        if (capabilityCode == null || capabilityCode.isBlank()) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "capability code is required");
        }
        return capabilityCatalogRepository
                .findByCode(capabilityCode.trim())
                .orElseThrow(() -> new BizException(ErrorCode.NOT_FOUND, "capability not found"));
    }

    private UUID resolveRoleBindingEnterpriseId(UserEntity targetUser, RoleTemplateEntity roleTemplate) {
        if (UserRole.ENTERPRISE_OWNER.getCode().equals(roleTemplate.getLegacyRoleCode())) {
            if (targetUser.getEnterpriseId() == null) {
                throw new BizException(
                        ErrorCode.INVALID_REQUEST,
                        "enterprise owner template requires an enterprise-scoped user");
            }
            return targetUser.getEnterpriseId();
        }
        if (targetUser.getEnterpriseId() != null
                && (UserRole.REVIEWER.getCode().equals(roleTemplate.getLegacyRoleCode())
                        || UserRole.OPERATIONS_ADMIN.getCode().equals(roleTemplate.getLegacyRoleCode()))) {
            throw new BizException(
                    ErrorCode.INVALID_REQUEST,
                    "platform role templates cannot be assigned to tenant-scoped users");
        }
        return null;
    }

    private PermissionCode parsePermission(String permissionCode) {
        if (permissionCode == null || permissionCode.isBlank()) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "permission code is required");
        }
        try {
            return PermissionCode.fromCode(permissionCode.trim());
        } catch (IllegalArgumentException exception) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "unsupported permission code");
        }
    }

    private DataScopeCode parseDataScope(String scopeType) {
        if (scopeType == null || scopeType.isBlank()) {
            return null;
        }
        try {
            return DataScopeCode.fromCode(scopeType.trim());
        } catch (IllegalArgumentException exception) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "unsupported scope type");
        }
    }

    private void validateReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BizException(ErrorCode.INVALID_REQUEST, "reason is required");
        }
    }

    private Range normalizeRange(OffsetDateTime effectiveFrom, OffsetDateTime expiresAt) {
        OffsetDateTime normalizedEffectiveFrom =
                effectiveFrom == null ? OffsetDateTime.now() : effectiveFrom;
        if (expiresAt != null && !expiresAt.isAfter(normalizedEffectiveFrom)) {
            throw new BizException(
                    ErrorCode.INVALID_REQUEST, "expiresAt must be after effectiveFrom");
        }
        return new Range(normalizedEffectiveFrom, expiresAt);
    }

    private boolean isActiveAt(OffsetDateTime expiresAt, OffsetDateTime now) {
        return expiresAt == null || expiresAt.isAfter(now);
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void validateTemporaryAccessScope(
            UserEntity targetUser, GrantTemporaryAccessCommand command, Range range) {
        if (range.expiresAt() == null) {
            throw new BizException(
                    ErrorCode.INVALID_REQUEST, "temporary access must define expiresAt");
        }

        boolean hasScopeType = command.scopeType() != null && !command.scopeType().isBlank();
        boolean hasScopeValue = command.scopeValue() != null && !command.scopeValue().isBlank();
        boolean hasResourceType = command.resourceType() != null && !command.resourceType().isBlank();
        boolean hasResourceId = command.resourceId() != null;

        if (command.enterpriseId() == null) {
            throw new BizException(
                    ErrorCode.INVALID_REQUEST,
                    "temporary access must be scoped to a target enterprise");
        }
        if (targetUser.getEnterpriseId() == null
                || !command.enterpriseId().equals(targetUser.getEnterpriseId())) {
            throw new BizException(
                    ErrorCode.INVALID_REQUEST,
                    "temporary access must match the target user's enterprise");
        }
        if (hasScopeType != hasScopeValue) {
            throw new BizException(
                    ErrorCode.INVALID_REQUEST, "scopeType and scopeValue must be provided together");
        }
        if (hasResourceType != hasResourceId) {
            throw new BizException(
                    ErrorCode.INVALID_REQUEST,
                    "resourceType and resourceId must be provided together");
        }
        if (hasScopeType || hasResourceType) {
            throw new BizException(
                    ErrorCode.INVALID_REQUEST,
                    "resource-scoped or data-scoped temporary access is not supported yet");
        }
    }

    private record Range(OffsetDateTime effectiveFrom, OffsetDateTime expiresAt) {}

    public record GrantRoleTemplateCommand(
            UUID targetUserId,
            String roleTemplateCode,
            String reason,
            OffsetDateTime effectiveFrom,
            OffsetDateTime expiresAt) {}

    public record GrantCapabilityBindingCommand(
            UUID targetUserId,
            String capabilityCode,
            String reason,
            OffsetDateTime effectiveFrom,
            OffsetDateTime expiresAt) {}

    public record GrantTemporaryAccessCommand(
            UUID targetUserId,
            String permissionCode,
            UUID enterpriseId,
            String scopeType,
            String scopeValue,
            String resourceType,
            UUID resourceId,
            String reason,
            String ticketNo,
            OffsetDateTime effectiveFrom,
            OffsetDateTime expiresAt) {}

    public record AuthorizationMutationResult(
            UUID id,
            String type,
            UUID targetUserId,
            String code,
            OffsetDateTime effectiveFrom,
            OffsetDateTime expiresAt,
            OffsetDateTime revokedAt) {

        static AuthorizationMutationResult fromRoleBinding(UserRoleBindingEntity binding, String code) {
            return new AuthorizationMutationResult(
                    binding.getId(),
                    TARGET_TYPE_ROLE_BINDING,
                    binding.getUserId(),
                    code,
                    binding.getEffectiveFrom(),
                    binding.getExpiresAt(),
                    binding.getRevokedAt());
        }

        static AuthorizationMutationResult fromCapabilityBinding(
                UserCapabilityBindingEntity binding, String code) {
            return new AuthorizationMutationResult(
                    binding.getId(),
                    TARGET_TYPE_CAPABILITY_BINDING,
                    binding.getUserId(),
                    code,
                    binding.getEffectiveFrom(),
                    binding.getExpiresAt(),
                    binding.getRevokedAt());
        }

        static AuthorizationMutationResult fromAccessGrant(AccessGrantEntity grant, String code) {
            return new AuthorizationMutationResult(
                    grant.getId(),
                    TARGET_TYPE_ACCESS_GRANT,
                    grant.getPrincipalId(),
                    code,
                    grant.getEffectiveFrom(),
                    grant.getExpiresAt(),
                    grant.getRevokedAt());
        }
    }
}
