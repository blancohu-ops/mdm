package com.industrial.mdm.modules.baseDictionary.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.industrial.mdm.common.security.JwtAuthenticationFilter;
import com.industrial.mdm.common.security.JwtTokenService;
import com.industrial.mdm.config.SecurityConfig;
import com.industrial.mdm.modules.auth.repository.UserRepository;
import com.industrial.mdm.modules.baseDictionary.application.AdministrativeRegionService;
import com.industrial.mdm.modules.baseDictionary.application.BaseDictionaryService;
import com.industrial.mdm.modules.baseDictionary.dto.DictTypeResponse;
import com.industrial.mdm.modules.baseDictionary.dto.RegionNodeResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({PublicDictionaryController.class, PublicRegionController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class BaseDictionaryPublicControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BaseDictionaryService baseDictionaryService;

    @MockBean
    private AdministrativeRegionService administrativeRegionService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void dictionaryEndpointAllowsAnonymousAccess() throws Exception {
        when(baseDictionaryService.getType("company_type", true))
                .thenReturn(
                        new DictTypeResponse(
                                "company_type",
                                "企业类型",
                                null,
                                true,
                                List.of(
                                        new DictTypeResponse.DictItemResponse(
                                                UUID.randomUUID(),
                                                "manufacturing",
                                                "生产制造企业",
                                                1,
                                                true))));

        mockMvc.perform(get("/api/v1/dictionaries/company_type")).andExpect(status().isOk());

        verify(baseDictionaryService).getType("company_type", true);
    }

    @Test
    void regionEndpointAllowsAnonymousAccess() throws Exception {
        when(administrativeRegionService.listPublicRegions(1, null))
                .thenReturn(
                        List.of(
                                new RegionNodeResponse(
                                        UUID.randomUUID(),
                                        "320000",
                                        "江苏省",
                                        1,
                                        null,
                                        10,
                                        true,
                                        List.of())));

        mockMvc.perform(get("/api/v1/regions").param("level", "1")).andExpect(status().isOk());

        verify(administrativeRegionService).listPublicRegions(1, null);
    }
}
