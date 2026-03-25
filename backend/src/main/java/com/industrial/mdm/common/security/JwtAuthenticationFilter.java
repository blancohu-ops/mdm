package com.industrial.mdm.common.security;

import com.industrial.mdm.modules.auth.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtTokenService jwtTokenService, UserRepository userRepository) {
        this.jwtTokenService = jwtTokenService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Optional<String> bearerToken = resolveBearerToken(request);
        if (bearerToken.isPresent()) {
            try {
                AuthenticatedUser user = jwtTokenService.parseAccessToken(bearerToken.get());
                userRepository
                        .findById(user.userId())
                        .filter(entity -> entity.getStatus().isActive())
                        .filter(
                                entity ->
                                        user.authzVersion()
                                                == normalizeAuthzVersion(entity.getAuthzVersion()))
                        .ifPresent(
                                entity -> {
                                    UsernamePasswordAuthenticationToken authentication =
                                            new UsernamePasswordAuthenticationToken(
                                                    user, null, user.getAuthorities());
                                    SecurityContextHolder.getContext()
                                            .setAuthentication(authentication);
                                });
            } catch (RuntimeException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private Optional<String> resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Optional.empty();
        }
        return Optional.of(authorization.substring(7));
    }

    private int normalizeAuthzVersion(Integer authzVersion) {
        return authzVersion == null ? 0 : authzVersion;
    }
}
