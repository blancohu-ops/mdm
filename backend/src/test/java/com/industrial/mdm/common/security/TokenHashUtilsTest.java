package com.industrial.mdm.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenHashUtilsTest {

    @Test
    void shouldGenerateStableHash() {
        String first = TokenHashUtils.sha256("demo-token");
        String second = TokenHashUtils.sha256("demo-token");

        assertThat(first).hasSize(64);
        assertThat(first).isEqualTo(second);
    }
}
