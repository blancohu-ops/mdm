package com.industrial.mdm.modules.file.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.industrial.mdm.common.security.JwtAuthenticationFilter;
import com.industrial.mdm.common.security.JwtTokenService;
import com.industrial.mdm.config.SecurityConfig;
import com.industrial.mdm.modules.auth.repository.UserRepository;
import com.industrial.mdm.modules.file.application.FileService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FileController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class FileControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileService fileService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void metadataRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/files/{fileId}", UUID.randomUUID())).andExpect(status().isUnauthorized());
    }

    @Test
    void downloadEndpointIsAnonymousReachableAtSecurityLayer() throws Exception {
        when(fileService.download(any(UUID.class), isNull()))
                .thenReturn(ResponseEntity.ok(new ByteArrayResource(new byte[0])));

        mockMvc.perform(get("/api/v1/files/{fileId}/download", UUID.randomUUID()))
                .andExpect(status().isOk());

        verify(fileService).download(any(UUID.class), isNull());
    }
}
