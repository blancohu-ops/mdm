package com.industrial.mdm.modules.iam.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.common.security.UserRole;
import com.industrial.mdm.modules.auth.domain.AccountStatus;
import com.industrial.mdm.modules.auth.repository.UserEntity;
import com.industrial.mdm.modules.auth.repository.UserRepository;
import com.industrial.mdm.modules.iam.domain.capability.CapabilityCode;
import com.industrial.mdm.modules.iam.domain.permission.PermissionCode;
import com.industrial.mdm.modules.iam.domain.scope.DataScopeCode;
import com.industrial.mdm.modules.iam.repository.AccessGrantEntity;
import com.industrial.mdm.modules.iam.repository.AccessGrantRepository;
import com.industrial.mdm.modules.iam.repository.CapabilityCatalogEntity;
import com.industrial.mdm.modules.iam.repository.CapabilityCatalogRepository;
import com.industrial.mdm.modules.iam.repository.RoleTemplateEntity;
import com.industrial.mdm.modules.iam.repository.RoleTemplateRepository;
import com.industrial.mdm.modules.iam.repository.UserCapabilityBindingEntity;
import com.industrial.mdm.modules.iam.repository.UserCapabilityBindingRepository;
import com.industrial.mdm.modules.iam.repository.UserRoleBindingEntity;
import com.industrial.mdm.modules.iam.repository.UserRoleBindingRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AuthorizationServicePersistenceTest {

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleTemplateRepository roleTemplateRepository;

    @Autowired
    private UserRoleBindingRepository userRoleBindingRepository;

    @Autowired
    private CapabilityCatalogRepository capabilityCatalogRepository;

    @Autowired
    private UserCapabilityBindingRepository userCapabilityBindingRepository;

    @Autowired
    private AccessGrantRepository accessGrantRepository;

    @Test
    void activeRoleBindingOverridesLegacyRoleFallback() {
        UserEntity user = saveUser(UserRole.ENTERPRISE_OWNER);
        RoleTemplateEntity operationsAdminTemplate =
                roleTemplateRepository.findByCode("operations_admin_base").orElseThrow();

        UserRoleBindingEntity binding = new UserRoleBindingEntity();
        binding.setUserId(user.getId());
        binding.setRoleTemplateId(operationsAdminTemplate.getId());
        binding.setSourceType("manual_assignment");
        binding.setPrimary(true);
        binding.setReason("test override");
        binding.setEffectiveFrom(OffsetDateTime.now().minusMinutes(1));
        userRoleBindingRepository.save(binding);

        AuthenticatedUser currentUser = toPrincipal(user);

        assertThat(
                        authorizationService.hasPermission(
                                currentUser, PermissionCode.COMPANY_MANAGE_FREEZE))
                .isTrue();
        assertThat(authorizationService.hasPermission(currentUser, PermissionCode.PRODUCT_UPDATE))
                .isFalse();
        assertThat(authorizationService.hasDataScope(currentUser, DataScopeCode.TENANT)).isFalse();
    }

    @Test
    void capabilityBindingAndTemporaryGrantAugmentResolvedAuthorization() {
        UserEntity user = saveUser(UserRole.ENTERPRISE_OWNER);
        CapabilityCatalogEntity advancedCapability =
                capabilityCatalogRepository.findByCode(CapabilityCode.AI_ADVANCED.getCode()).orElseThrow();

        UserCapabilityBindingEntity capabilityBinding = new UserCapabilityBindingEntity();
        capabilityBinding.setUserId(user.getId());
        capabilityBinding.setCapabilityId(advancedCapability.getId());
        capabilityBinding.setSourceType("manual_assignment");
        capabilityBinding.setReason("test capability");
        capabilityBinding.setEffectiveFrom(OffsetDateTime.now().minusMinutes(1));
        userCapabilityBindingRepository.save(capabilityBinding);

        AccessGrantEntity grant = new AccessGrantEntity();
        grant.setPrincipalType("user");
        grant.setPrincipalId(user.getId());
        grant.setPermissionCode(PermissionCode.AI_TOOL_WRITEBACK.getCode());
        grant.setScopeType(DataScopeCode.TEMP_GRANTED.getCode());
        grant.setGrantType("temporary_access");
        grant.setEffect("allow");
        grant.setReason("test temp grant");
        grant.setEffectiveFrom(OffsetDateTime.now().minusMinutes(1));
        accessGrantRepository.save(grant);

        AuthenticatedUser currentUser = toPrincipal(user);

        assertThat(authorizationService.hasPermission(currentUser, PermissionCode.PRODUCT_UPDATE))
                .isTrue();
        assertThat(authorizationService.hasCapability(currentUser, CapabilityCode.AI_ADVANCED))
                .isTrue();
        assertThat(
                        authorizationService.hasPermission(
                                currentUser, PermissionCode.AI_TOOL_WRITEBACK))
                .isTrue();
        assertThat(authorizationService.hasDataScope(currentUser, DataScopeCode.TEMP_GRANTED))
                .isTrue();
    }

    private UserEntity saveUser(UserRole role) {
        UserEntity user = new UserEntity();
        user.setAccount("authz-" + UUID.randomUUID());
        user.setPhone("139" + Math.abs(UUID.randomUUID().hashCode()));
        user.setEmail("authz-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("encoded-password");
        user.setRole(role);
        user.setStatus(AccountStatus.ACTIVE);
        user.setDisplayName("authz-user");
        user.setOrganization("platform");
        user.setAuthzVersion(0);
        return userRepository.save(user);
    }

    private AuthenticatedUser toPrincipal(UserEntity user) {
        return new AuthenticatedUser(
                user.getId(),
                user.getRole(),
                user.getEnterpriseId(),
                user.getDisplayName(),
                user.getOrganization(),
                user.getAuthzVersion() == null ? 0 : user.getAuthzVersion());
    }
}
