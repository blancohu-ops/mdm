package com.industrial.mdm.modules.iam.application;

import com.industrial.mdm.modules.auth.repository.UserEntity;
import com.industrial.mdm.modules.auth.repository.UserRepository;
import com.industrial.mdm.modules.iam.repository.RoleTemplateEntity;
import com.industrial.mdm.modules.iam.repository.RoleTemplateRepository;
import com.industrial.mdm.modules.iam.repository.UserRoleBindingEntity;
import com.industrial.mdm.modules.iam.repository.UserRoleBindingRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LegacyUserRoleBindingInitializer {

    static final String SOURCE_TYPE_LEGACY_ROLE = "legacy_role";
    private static final String REVOKE_REASON = "legacy role binding superseded by sync";
    private static final String CREATE_REASON = "initialized from users.role";

    private final UserRepository userRepository;
    private final RoleTemplateRepository roleTemplateRepository;
    private final UserRoleBindingRepository userRoleBindingRepository;
    private final AuthorizationStateService authorizationStateService;

    public LegacyUserRoleBindingInitializer(
            UserRepository userRepository,
            RoleTemplateRepository roleTemplateRepository,
            UserRoleBindingRepository userRoleBindingRepository,
            AuthorizationStateService authorizationStateService) {
        this.userRepository = userRepository;
        this.roleTemplateRepository = roleTemplateRepository;
        this.userRoleBindingRepository = userRoleBindingRepository;
        this.authorizationStateService = authorizationStateService;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onApplicationReady() {
        synchronizeLegacyBindings();
    }

    @Transactional
    public void synchronizeLegacyBindings() {
        for (UserEntity user : userRepository.findAll()) {
            if (user.getRole() == null) {
                continue;
            }
            RoleTemplateEntity roleTemplate =
                    roleTemplateRepository
                            .findByLegacyRoleCode(user.getRole().getCode())
                            .orElseThrow(
                                    () ->
                                            new IllegalStateException(
                                                    "missing role template for legacy role "
                                                            + user.getRole().getCode()));
            boolean changed = synchronizeUserLegacyBinding(user, roleTemplate);
            if (changed) {
                authorizationStateService.invalidateUserAuthorization(user);
            }
        }
    }

    private boolean synchronizeUserLegacyBinding(UserEntity user, RoleTemplateEntity roleTemplate) {
        List<UserRoleBindingEntity> activeBindings =
                userRoleBindingRepository.findByUserIdAndSourceTypeAndRevokedAtIsNull(
                        user.getId(), SOURCE_TYPE_LEGACY_ROLE);
        OffsetDateTime now = OffsetDateTime.now();

        boolean hasExpectedBinding = false;
        boolean changed = false;
        for (UserRoleBindingEntity binding : activeBindings) {
            if (matches(binding, roleTemplate.getId(), user.getEnterpriseId())) {
                hasExpectedBinding = true;
                continue;
            }
            binding.setRevokedAt(now);
            binding.setRevokedReason(REVOKE_REASON);
            userRoleBindingRepository.save(binding);
            changed = true;
        }

        if (!hasExpectedBinding) {
            UserRoleBindingEntity binding = new UserRoleBindingEntity();
            binding.setUserId(user.getId());
            binding.setRoleTemplateId(roleTemplate.getId());
            binding.setEnterpriseId(user.getEnterpriseId());
            binding.setSourceType(SOURCE_TYPE_LEGACY_ROLE);
            binding.setPrimary(true);
            binding.setReason(CREATE_REASON);
            binding.setEffectiveFrom(
                    user.getCreatedAt() == null ? now : user.getCreatedAt());
            userRoleBindingRepository.save(binding);
            changed = true;
        }
        return changed;
    }

    private boolean matches(
            UserRoleBindingEntity binding, UUID roleTemplateId, UUID enterpriseId) {
        return binding.getRoleTemplateId().equals(roleTemplateId)
                && Objects.equals(binding.getEnterpriseId(), enterpriseId);
    }
}
