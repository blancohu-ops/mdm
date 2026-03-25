package com.industrial.mdm.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.industrial.mdm.modules.auth.domain.AccountStatus;
import com.industrial.mdm.modules.auth.repository.UserEntity;
import com.industrial.mdm.modules.auth.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private UserRepository userRepository;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenService, userRepository);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void matchingAuthzVersionAuthenticatesRequest() throws Exception {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser authenticatedUser =
                new AuthenticatedUser(
                        userId, UserRole.REVIEWER, null, "reviewer", "platform", 2);
        UserEntity entity = userEntity(userId, 2);
        MockHttpServletRequest request = bearerRequest();

        when(jwtTokenService.parseAccessToken("token")).thenReturn(authenticatedUser);
        when(userRepository.findById(userId)).thenReturn(Optional.of(entity));

        jwtAuthenticationFilter.doFilterInternal(
                request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo(authenticatedUser);
    }

    @Test
    void staleAuthzVersionLeavesRequestAnonymous() throws Exception {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser authenticatedUser =
                new AuthenticatedUser(
                        userId, UserRole.REVIEWER, null, "reviewer", "platform", 1);
        UserEntity entity = userEntity(userId, 2);
        MockHttpServletRequest request = bearerRequest();

        when(jwtTokenService.parseAccessToken("token")).thenReturn(authenticatedUser);
        when(userRepository.findById(userId)).thenReturn(Optional.of(entity));

        jwtAuthenticationFilter.doFilterInternal(
                request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private MockHttpServletRequest bearerRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        return request;
    }

    private UserEntity userEntity(UUID userId, int authzVersion) throws Exception {
        UserEntity entity = new UserEntity();
        setField(UserEntity.class, entity, "id", userId);
        entity.setStatus(AccountStatus.ACTIVE);
        entity.setAuthzVersion(authzVersion);
        return entity;
    }

    private void setField(Class<?> type, Object target, String fieldName, Object value)
            throws Exception {
        var field = type.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
