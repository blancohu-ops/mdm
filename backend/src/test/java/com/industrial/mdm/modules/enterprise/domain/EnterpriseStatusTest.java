package com.industrial.mdm.modules.enterprise.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EnterpriseStatusTest {

    @Test
    void shouldAllowEditForApprovedAndRejected() {
        assertThat(EnterpriseStatus.APPROVED.canEdit()).isTrue();
        assertThat(EnterpriseStatus.REJECTED.canEdit()).isTrue();
        assertThat(EnterpriseStatus.PENDING_REVIEW.canEdit()).isFalse();
    }
}
