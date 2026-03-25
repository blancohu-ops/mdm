package com.industrial.mdm.modules.iam.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.industrial.mdm.common.security.UserRole;
import com.industrial.mdm.modules.iam.domain.capability.CapabilityCode;
import com.industrial.mdm.modules.iam.domain.permission.PermissionCode;
import com.industrial.mdm.modules.iam.domain.role.RoleTemplatePolicy;
import com.industrial.mdm.modules.iam.domain.scope.DataScopeCode;
import com.industrial.mdm.modules.iam.repository.CapabilityCatalogRepository;
import com.industrial.mdm.modules.iam.repository.DataScopeCatalogRepository;
import com.industrial.mdm.modules.iam.repository.PermissionCatalogRepository;
import com.industrial.mdm.modules.iam.repository.RoleTemplateCapabilityRepository;
import com.industrial.mdm.modules.iam.repository.RoleTemplateEntity;
import com.industrial.mdm.modules.iam.repository.RoleTemplatePermissionRepository;
import com.industrial.mdm.modules.iam.repository.RoleTemplateRepository;
import com.industrial.mdm.modules.iam.repository.RoleTemplateScopeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BuiltInAuthorizationCatalogPersistenceTest {

    @Autowired
    private RoleAuthorizationCatalog roleAuthorizationCatalog;

    @Autowired
    private RoleTemplateRepository roleTemplateRepository;

    @Autowired
    private PermissionCatalogRepository permissionCatalogRepository;

    @Autowired
    private CapabilityCatalogRepository capabilityCatalogRepository;

    @Autowired
    private DataScopeCatalogRepository dataScopeCatalogRepository;

    @Autowired
    private RoleTemplatePermissionRepository roleTemplatePermissionRepository;

    @Autowired
    private RoleTemplateCapabilityRepository roleTemplateCapabilityRepository;

    @Autowired
    private RoleTemplateScopeRepository roleTemplateScopeRepository;

    @Test
    void builtInCatalogEntriesExistInDatabase() {
        assertThat(permissionCatalogRepository.findByCode(PermissionCode.FILE_ASSET_UPLOAD.getCode()))
                .isPresent();
        assertThat(
                        capabilityCatalogRepository.findByCode(
                                CapabilityCode.AI_ADVANCED.getCode()))
                .isPresent();
        assertThat(dataScopeCatalogRepository.findByCode(DataScopeCode.TENANT.getCode())).isPresent();
    }

    @Test
    void builtInRoleTemplatesAreMaterializedWithExpectedBindings() {
        for (UserRole role : UserRole.values()) {
            RoleTemplateEntity roleTemplate =
                    roleTemplateRepository.findByLegacyRoleCode(role.getCode()).orElseThrow();
            RoleTemplatePolicy policy = roleAuthorizationCatalog.getRequired(role);

            assertThat(roleTemplate.isBuiltIn()).isTrue();
            assertThat(roleTemplatePermissionRepository.countByRoleTemplateId(roleTemplate.getId()))
                    .isEqualTo(policy.permissions().size());
            assertThat(roleTemplateCapabilityRepository.countByRoleTemplateId(roleTemplate.getId()))
                    .isEqualTo(policy.capabilities().size());
            assertThat(roleTemplateScopeRepository.countByRoleTemplateId(roleTemplate.getId()))
                    .isEqualTo(policy.dataScopes().size());
        }
    }
}
