package com.industrial.mdm.modules.iam.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AccessGrantRepositoryTest {

    @Autowired
    private AccessGrantRepository accessGrantRepository;

    @Test
    void findActiveGrantsReturnsOnlyCurrentNonRevokedGrants() {
        UUID principalId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        AccessGrantEntity activeGrant = grant(principalId, now.minusHours(1), null, null);
        activeGrant.setPermissionCode("product:read");
        accessGrantRepository.save(activeGrant);

        AccessGrantEntity expiredGrant = grant(principalId, now.minusHours(2), now.minusMinutes(1), null);
        expiredGrant.setPermissionCode("product:update");
        accessGrantRepository.save(expiredGrant);

        AccessGrantEntity revokedGrant = grant(principalId, now.minusHours(2), null, now.minusMinutes(5));
        revokedGrant.setPermissionCode("product:submit");
        accessGrantRepository.save(revokedGrant);

        AccessGrantEntity futureGrant = grant(principalId, now.plusMinutes(5), null, null);
        futureGrant.setPermissionCode("product:delete");
        accessGrantRepository.save(futureGrant);

        AccessGrantEntity anotherPrincipalGrant = grant(UUID.randomUUID(), now.minusMinutes(10), null, null);
        anotherPrincipalGrant.setPermissionCode("product:export");
        accessGrantRepository.save(anotherPrincipalGrant);

        AccessGrantEntity anotherPrincipalTypeGrant = grant(principalId, now.minusMinutes(10), null, null);
        anotherPrincipalTypeGrant.setPrincipalType("group");
        anotherPrincipalTypeGrant.setPermissionCode("product:group_scope");
        accessGrantRepository.save(anotherPrincipalTypeGrant);

        assertThat(accessGrantRepository.findActiveGrants("user", principalId, now))
                .extracting(AccessGrantEntity::getPermissionCode)
                .containsExactly("product:read");
    }

    @Test
    void findActiveGrantsHonorsBoundaryTimes() {
        UUID principalId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        AccessGrantEntity effectiveAtNow = grant(principalId, now, now.plusMinutes(5), null);
        effectiveAtNow.setPermissionCode("grant:effective_now");
        accessGrantRepository.save(effectiveAtNow);

        AccessGrantEntity expiresAtNow = grant(principalId, now.minusMinutes(5), now, null);
        expiresAtNow.setPermissionCode("grant:expires_now");
        accessGrantRepository.save(expiresAtNow);

        assertThat(accessGrantRepository.findActiveGrants("user", principalId, now))
                .extracting(AccessGrantEntity::getPermissionCode)
                .contains("grant:effective_now")
                .doesNotContain("grant:expires_now");
    }

    private AccessGrantEntity grant(
            UUID principalId,
            OffsetDateTime effectiveFrom,
            OffsetDateTime expiresAt,
            OffsetDateTime revokedAt) {
        AccessGrantEntity entity = new AccessGrantEntity();
        entity.setPrincipalType("user");
        entity.setPrincipalId(principalId);
        entity.setPermissionCode("placeholder:permission");
        entity.setGrantType("temporary");
        entity.setEffect("allow");
        entity.setReason("test");
        entity.setEffectiveFrom(effectiveFrom);
        entity.setExpiresAt(expiresAt);
        entity.setRevokedAt(revokedAt);
        return entity;
    }
}
