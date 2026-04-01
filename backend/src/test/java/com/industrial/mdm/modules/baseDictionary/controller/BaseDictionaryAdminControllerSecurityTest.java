package com.industrial.mdm.modules.baseDictionary.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.industrial.mdm.common.security.AuthenticatedUser;
import com.industrial.mdm.common.security.JwtAuthenticationFilter;
import com.industrial.mdm.common.security.JwtTokenService;
import com.industrial.mdm.common.security.UserRole;
import com.industrial.mdm.config.SecurityConfig;
import com.industrial.mdm.modules.auth.repository.UserRepository;
import com.industrial.mdm.modules.baseDictionary.application.AdministrativeRegionService;
import com.industrial.mdm.modules.baseDictionary.application.BaseDictionaryService;
import com.industrial.mdm.modules.baseDictionary.dto.DictItemSaveRequest;
import com.industrial.mdm.modules.baseDictionary.dto.DictTypeResponse;
import com.industrial.mdm.modules.baseDictionary.dto.RegionNodeResponse;
import com.industrial.mdm.modules.baseDictionary.dto.RegionSaveRequest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({AdminDictionaryController.class, AdminRegionController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class BaseDictionaryAdminControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BaseDictionaryService baseDictionaryService;

    @MockBean
    private AdministrativeRegionService administrativeRegionService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void adminEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dictionaries")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpointsRejectEnterpriseOwner() throws Exception {
        mockMvc.perform(
                        get("/api/v1/admin/dictionaries")
                                .with(authentication(authenticationFor(UserRole.ENTERPRISE_OWNER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void operationsAdminCanReadAndMutateBaseData() throws Exception {
        AuthenticatedUser currentUser = authenticatedUser(UserRole.OPERATIONS_ADMIN);
        when(baseDictionaryService.listTypes()).thenReturn(List.of());
        when(baseDictionaryService.createItem(
                        "company_type",
                        new DictItemSaveRequest("machinery", "机械企业", 10, true)))
                .thenReturn(
                        new DictTypeResponse.DictItemResponse(
                                UUID.randomUUID(), "machinery", "机械企业", 10, true));
        when(administrativeRegionService.create(
                        new RegionSaveRequest("329900", "测试地区", 2, "320000", 99)))
                .thenReturn(
                        new RegionNodeResponse(
                                UUID.randomUUID(),
                                "329900",
                                "测试地区",
                                2,
                                "320000",
                                99,
                                true,
                                List.of()));

        mockMvc.perform(
                        get("/api/v1/admin/dictionaries")
                                .with(authentication(authenticationFor(currentUser))))
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/api/v1/admin/dictionaries/company_type/items")
                                .with(authentication(authenticationFor(currentUser)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new DictItemSaveRequest(
                                                        "machinery", "机械企业", 10, true))))
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/api/v1/admin/regions")
                                .with(authentication(authenticationFor(currentUser)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new RegionSaveRequest(
                                                        "329900", "测试地区", 2, "320000", 99))))
                .andExpect(status().isOk());

        verify(baseDictionaryService).listTypes();
        verify(baseDictionaryService)
                .createItem("company_type", new DictItemSaveRequest("machinery", "机械企业", 10, true));
        verify(administrativeRegionService)
                .create(new RegionSaveRequest("329900", "测试地区", 2, "320000", 99));
    }

    private UsernamePasswordAuthenticationToken authenticationFor(UserRole role) {
        return authenticationFor(authenticatedUser(role));
    }

    private UsernamePasswordAuthenticationToken authenticationFor(AuthenticatedUser currentUser) {
        return new UsernamePasswordAuthenticationToken(
                currentUser, null, currentUser.getAuthorities());
    }

    private AuthenticatedUser authenticatedUser(UserRole role) {
        return new AuthenticatedUser(
                UUID.randomUUID(),
                role,
                role == UserRole.ENTERPRISE_OWNER ? UUID.randomUUID() : null,
                null,
                "tester",
                "org",
                0);
    }
}
