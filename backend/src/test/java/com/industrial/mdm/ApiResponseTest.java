package com.industrial.mdm;

import static org.assertj.core.api.Assertions.assertThat;

import com.industrial.mdm.common.api.ApiResponse;
import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void shouldBuildSuccessResponse() {
        ApiResponse<String> response = ApiResponse.success("ok", "req_test");

        assertThat(response.code()).isEqualTo("OK");
        assertThat(response.message()).isEqualTo("success");
        assertThat(response.data()).isEqualTo("ok");
        assertThat(response.requestId()).isEqualTo("req_test");
    }
}
