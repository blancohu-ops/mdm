package com.industrial.mdm.modules.userManagement.dto;

import java.util.List;
import java.util.UUID;

public record AdminUserOptionsResponse(
        List<EnterpriseOption> enterprises,
        List<RoleTemplateOption> roleTemplates,
        List<CapabilityOption> capabilities) {

    public record EnterpriseOption(UUID id, String name, String status) {}

    public record RoleTemplateOption(
            String code, String name, String legacyRoleCode, boolean builtIn) {}

    public record CapabilityOption(String code, String description) {}
}
