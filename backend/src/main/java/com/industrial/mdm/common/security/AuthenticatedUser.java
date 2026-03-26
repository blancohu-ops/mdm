package com.industrial.mdm.common.security;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record AuthenticatedUser(
        UUID userId,
        UserRole role,
        UUID enterpriseId,
        UUID serviceProviderId,
        String displayName,
        String organization,
        int authzVersion)
        implements Principal {

    @Override
    public String getName() {
        return displayName;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.getCode()));
    }
}
