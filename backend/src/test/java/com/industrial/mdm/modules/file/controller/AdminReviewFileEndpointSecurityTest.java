package com.industrial.mdm.modules.file.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.industrial.mdm.common.security.JwtAuthenticationFilter;
import com.industrial.mdm.common.security.JwtTokenService;
import com.industrial.mdm.config.SecurityConfig;
import com.industrial.mdm.modules.auth.repository.UserRepository;
import com.industrial.mdm.modules.enterpriseReview.application.EnterpriseReviewService;
import com.industrial.mdm.modules.enterpriseReview.controller.AdminCompanyReviewController;
import com.industrial.mdm.modules.file.application.ReviewContextFileAccessService;
import com.industrial.mdm.modules.productReview.application.ProductReviewService;
import com.industrial.mdm.modules.productReview.controller.AdminProductReviewController;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({AdminCompanyReviewController.class, AdminProductReviewController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AdminReviewFileEndpointSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EnterpriseReviewService enterpriseReviewService;

    @MockBean
    private ProductReviewService productReviewService;

    @MockBean
    private ReviewContextFileAccessService reviewContextFileAccessService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void companyReviewFileEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(
                        get(
                                "/api/v1/admin/company-reviews/{enterpriseId}/files/{fileId}/download",
                                UUID.randomUUID(),
                                UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void companyReviewFileEndpointRejectsEnterpriseRole() throws Exception {
        mockMvc.perform(
                        get(
                                        "/api/v1/admin/company-reviews/{enterpriseId}/files/{fileId}/download",
                                        UUID.randomUUID(),
                                        UUID.randomUUID())
                                .with(user("enterprise").authorities(() -> "enterprise_owner")))
                .andExpect(status().isForbidden());
    }

    @Test
    void productReviewFileEndpointAllowsReviewerRoleToReachServiceLayer() throws Exception {
        when(reviewContextFileAccessService.downloadProductReviewFile(any(), any(), isNull()))
                .thenReturn(ResponseEntity.ok(new ByteArrayResource(new byte[0])));

        mockMvc.perform(
                        get(
                                        "/api/v1/admin/product-reviews/{productId}/files/{fileId}/download",
                                        UUID.randomUUID(),
                                        UUID.randomUUID())
                                .with(user("reviewer").authorities(() -> "reviewer")))
                .andExpect(status().isOk());

        verify(reviewContextFileAccessService).downloadProductReviewFile(any(), any(), isNull());
    }
}
