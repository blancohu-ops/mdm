package com.industrial.mdm.modules.iam.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.industrial.mdm.common.security.UserRole;
import com.industrial.mdm.modules.auth.domain.AccountStatus;
import com.industrial.mdm.modules.auth.repository.UserEntity;
import com.industrial.mdm.modules.auth.repository.UserRepository;
import com.industrial.mdm.modules.iam.repository.RoleTemplateEntity;
import com.industrial.mdm.modules.iam.repository.RoleTemplateRepository;
import com.industrial.mdm.modules.iam.repository.UserRoleBindingEntity;
import com.industrial.mdm.modules.iam.repository.UserRoleBindingRepository;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class LegacyUserRoleBindingInitializerPersistenceTest {

    @Autowired
    private LegacyUserRoleBindingInitializer legacyUserRoleBindingInitializer;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleTemplateRepository roleTemplateRepository;

    @Autowired
    private UserRoleBindingRepository userRoleBindingRepository;

    @Test
    void synchronizeLegacyBindingsCreatesMatchingBindingForLegacyUser() {
        UserEntity user = saveUser(UserRole.ENTERPRISE_OWNER);

        legacyUserRoleBindingInitializer.synchronizeLegacyBindings();

        RoleTemplateEntity roleTemplate =
                roleTemplateRepository
                        .findByLegacyRoleCode(UserRole.ENTERPRISE_OWNER.getCode())
                        .orElseThrow();
        List<UserRoleBindingEntity> bindings =
                userRoleBindingRepository.findByUserIdAndSourceTypeAndRevokedAtIsNull(
                        user.getId(), LegacyUserRoleBindingInitializer.SOURCE_TYPE_LEGACY_ROLE);

        assertThat(bindings)
                .filteredOn(
                        binding ->
                                binding.getRoleTemplateId().equals(roleTemplate.getId())
                                        && Objects.equals(
                                                binding.getEnterpriseId(), user.getEnterpriseId()))
                .singleElement()
                .satisfies(
                        binding -> {
                            assertThat(binding.isPrimary()).isTrue();
                            assertThat(binding.getReason()).isEqualTo("initialized from users.role");
                            assertThat(binding.getEffectiveFrom()).isNotNull();
                        });
    }

    @Test
    void synchronizeLegacyBindingsCreatesMissingBindingAndBumpsAuthzVersion() {
        UserEntity user = saveUser(UserRole.REVIEWER);

        assertThat(
                        userRoleBindingRepository.findByUserIdAndSourceTypeAndRevokedAtIsNull(
                                user.getId(), LegacyUserRoleBindingInitializer.SOURCE_TYPE_LEGACY_ROLE))
                .isEmpty();

        legacyUserRoleBindingInitializer.synchronizeLegacyBindings();

        UserEntity reloadedUser = userRepository.findById(user.getId()).orElseThrow();
        RoleTemplateEntity roleTemplate =
                roleTemplateRepository.findByLegacyRoleCode(UserRole.REVIEWER.getCode()).orElseThrow();
        List<UserRoleBindingEntity> bindings =
                userRoleBindingRepository.findByUserIdAndSourceTypeAndRevokedAtIsNull(
                        user.getId(), LegacyUserRoleBindingInitializer.SOURCE_TYPE_LEGACY_ROLE);

        assertThat(reloadedUser.getAuthzVersion()).isEqualTo(1);
        assertThat(bindings)
                .filteredOn(binding -> binding.getRoleTemplateId().equals(roleTemplate.getId()))
                .hasSize(1);
    }

    @Test
    void synchronizeLegacyBindingsIsIdempotentWhenBindingAlreadyMatches() {
        UserEntity user = saveUser(UserRole.REVIEWER);

        legacyUserRoleBindingInitializer.synchronizeLegacyBindings();
        legacyUserRoleBindingInitializer.synchronizeLegacyBindings();

        UserEntity reloadedUser = userRepository.findById(user.getId()).orElseThrow();

        assertThat(reloadedUser.getAuthzVersion()).isEqualTo(1);
        assertThat(
                        userRoleBindingRepository.findByUserIdAndSourceTypeAndRevokedAtIsNull(
                                user.getId(), LegacyUserRoleBindingInitializer.SOURCE_TYPE_LEGACY_ROLE))
                .hasSize(1);
    }

    @Test
    void synchronizeLegacyBindingsRevokesStaleBindingWhenRoleChanges() {
        UserEntity user = saveUser(UserRole.REVIEWER);

        legacyUserRoleBindingInitializer.synchronizeLegacyBindings();

        user.setRole(UserRole.OPERATIONS_ADMIN);
        userRepository.save(user);

        legacyUserRoleBindingInitializer.synchronizeLegacyBindings();

        UserEntity reloadedUser = userRepository.findById(user.getId()).orElseThrow();
        RoleTemplateEntity currentTemplate =
                roleTemplateRepository
                        .findByLegacyRoleCode(UserRole.OPERATIONS_ADMIN.getCode())
                        .orElseThrow();
        List<UserRoleBindingEntity> activeBindings =
                userRoleBindingRepository.findByUserIdAndSourceTypeAndRevokedAtIsNull(
                        user.getId(), LegacyUserRoleBindingInitializer.SOURCE_TYPE_LEGACY_ROLE);
        List<UserRoleBindingEntity> allBindings =
                userRoleBindingRepository.findAll().stream()
                        .filter(binding -> binding.getUserId().equals(user.getId()))
                        .filter(
                                binding ->
                                        LegacyUserRoleBindingInitializer.SOURCE_TYPE_LEGACY_ROLE.equals(
                                                binding.getSourceType()))
                        .toList();

        assertThat(reloadedUser.getAuthzVersion()).isEqualTo(2);
        assertThat(activeBindings)
                .filteredOn(binding -> binding.getRoleTemplateId().equals(currentTemplate.getId()))
                .hasSize(1);
        assertThat(allBindings)
                .filteredOn(binding -> binding.getRevokedAt() != null)
                .singleElement()
                .satisfies(
                        binding ->
                                assertThat(binding.getRevokedReason())
                                        .isEqualTo("legacy role binding superseded by sync"));
    }

    private UserEntity saveUser(UserRole role) {
        UserEntity user = new UserEntity();
        user.setAccount("legacy-" + UUID.randomUUID());
        user.setPhone("139" + Math.abs(UUID.randomUUID().hashCode()));
        user.setEmail("legacy-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("encoded-password");
        user.setRole(role);
        user.setStatus(AccountStatus.ACTIVE);
        user.setDisplayName("reviewer");
        user.setOrganization("platform");
        user.setAuthzVersion(0);
        return userRepository.save(user);
    }
}
