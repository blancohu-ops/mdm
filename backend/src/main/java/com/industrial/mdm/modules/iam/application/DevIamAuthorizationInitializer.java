package com.industrial.mdm.modules.iam.application;

import com.industrial.mdm.modules.auth.repository.UserEntity;
import com.industrial.mdm.modules.auth.repository.UserRepository;
import com.industrial.mdm.modules.enterprise.repository.EnterpriseRepository;
import com.industrial.mdm.modules.iam.domain.context.ReviewDomainType;
import com.industrial.mdm.modules.iam.domain.permission.PermissionCode;
import com.industrial.mdm.modules.iam.repository.AccessGrantEntity;
import com.industrial.mdm.modules.iam.repository.AccessGrantRepository;
import com.industrial.mdm.modules.iam.repository.ReviewDomainAssignmentEntity;
import com.industrial.mdm.modules.iam.repository.ReviewDomainAssignmentRepository;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class DevIamAuthorizationInitializer {

    private static final String DEV_ADMIN_ACCOUNT = "admin@example.com";
    private static final String DEV_BOOTSTRAP_GRANT_TYPE = "dev_bootstrap";

    private final UserRepository userRepository;
    private final AccessGrantRepository accessGrantRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final ReviewDomainAssignmentRepository reviewDomainAssignmentRepository;
    private final AuthorizationStateService authorizationStateService;

    public DevIamAuthorizationInitializer(
            UserRepository userRepository,
            AccessGrantRepository accessGrantRepository,
            EnterpriseRepository enterpriseRepository,
            ReviewDomainAssignmentRepository reviewDomainAssignmentRepository,
            AuthorizationStateService authorizationStateService) {
        this.userRepository = userRepository;
        this.accessGrantRepository = accessGrantRepository;
        this.enterpriseRepository = enterpriseRepository;
        this.reviewDomainAssignmentRepository = reviewDomainAssignmentRepository;
        this.authorizationStateService = authorizationStateService;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onApplicationReady() {
        userRepository.findByAccountIgnoreCase(DEV_ADMIN_ACCOUNT).ifPresent(this::seedAdminBootstrap);
    }

    private void seedAdminBootstrap(UserEntity adminUser) {
        boolean changed = seedAdminPermissions(adminUser);
        changed = seedAdminReviewDomains(adminUser) || changed;
        if (changed) {
            authorizationStateService.invalidateUserAuthorization(adminUser);
        }
    }

    private boolean seedAdminPermissions(UserEntity adminUser) {
        OffsetDateTime now = OffsetDateTime.now();
        Set<String> existingPermissions =
                accessGrantRepository
                        .findActiveGrants(
                                AuthorizationAdministrationService.PRINCIPAL_TYPE_USER,
                                adminUser.getId(),
                                now)
                        .stream()
                        .map(AccessGrantEntity::getPermissionCode)
                        .collect(Collectors.toSet());

        boolean changed = false;
        for (PermissionCode permissionCode :
                Set.of(
                        PermissionCode.ROLE_TEMPLATE_GRANT,
                        PermissionCode.CAPABILITY_BINDING_GRANT,
                        PermissionCode.REVIEW_DOMAIN_ASSIGNMENT_MANAGE,
                        PermissionCode.ACCESS_GRANT_REQUEST_SUBMIT,
                        PermissionCode.ACCESS_GRANT_REQUEST_APPROVE,
                        PermissionCode.ACCESS_GRANT_MANAGE,
                        PermissionCode.AUDIT_LOG_READ)) {
            if (existingPermissions.contains(permissionCode.getCode())) {
                continue;
            }
            AccessGrantEntity grant = new AccessGrantEntity();
            grant.setPrincipalType(AuthorizationAdministrationService.PRINCIPAL_TYPE_USER);
            grant.setPrincipalId(adminUser.getId());
            grant.setPermissionCode(permissionCode.getCode());
            grant.setGrantType(DEV_BOOTSTRAP_GRANT_TYPE);
            grant.setEffect(AuthorizationAdministrationService.EFFECT_ALLOW);
            grant.setGrantedBy(adminUser.getId());
            grant.setApprovedBy(adminUser.getId());
            grant.setReason("bootstrap IAM admin permissions in dev");
            grant.setEffectiveFrom(now);
            accessGrantRepository.save(grant);
            changed = true;
        }
        return changed;
    }

    private boolean seedAdminReviewDomains(UserEntity adminUser) {
        OffsetDateTime now = OffsetDateTime.now();
        Set<String> existingAssignments =
                reviewDomainAssignmentRepository.findByUserIdAndRevokedAtIsNull(adminUser.getId()).stream()
                        .filter(assignment -> assignment.getExpiresAt() == null || assignment.getExpiresAt().isAfter(now))
                        .map(
                                assignment ->
                                        assignment.getDomainType()
                                                + ":"
                                                + assignment.getEnterpriseId())
                        .collect(Collectors.toSet());

        boolean changed = false;
        for (var enterprise : enterpriseRepository.findAll()) {
            for (ReviewDomainType domainType : ReviewDomainType.values()) {
                String key = domainType.getCode() + ":" + enterprise.getId();
                if (existingAssignments.contains(key)) {
                    continue;
                }
                ReviewDomainAssignmentEntity assignment = new ReviewDomainAssignmentEntity();
                assignment.setUserId(adminUser.getId());
                assignment.setDomainType(domainType.getCode());
                assignment.setEnterpriseId(enterprise.getId());
                assignment.setGrantedBy(adminUser.getId());
                assignment.setReason("bootstrap review domain access in dev");
                assignment.setEffectiveFrom(now);
                reviewDomainAssignmentRepository.save(assignment);
                changed = true;
            }
        }
        return changed;
    }
}
