package com.industrial.mdm.config;

import com.industrial.mdm.common.security.JwtAuthenticationFilter;
import com.industrial.mdm.common.security.PermissionSecurity;
import com.industrial.mdm.modules.iam.application.AuthorizationService;
import com.industrial.mdm.modules.iam.application.RoleAuthorizationCatalog;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(
                        session ->
                                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        registry ->
                                registry.requestMatchers(
                                                HttpMethod.OPTIONS,
                                                "/**")
                                        .permitAll()
                                        .requestMatchers(
                                                "/actuator/health",
                                                "/api/v1/system/ping",
                                                "/api/v1/auth/send-sms-code",
                                                "/api/v1/auth/register",
                                                "/api/v1/auth/login",
                                                "/api/v1/auth/refresh-token",
                                                "/api/v1/auth/reset-password",
                                                "/api/v1/auth/activation-links/**",
                                                "/api/v1/public/**",
                                                "/api/v1/files/*/download",
                                                "/api/v1/portal/**",
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .exceptionHandling(
                        exception ->
                                exception.authenticationEntryPoint(
                                                (request, response, authException) ->
                                                        response.sendError(
                                                                HttpServletResponse.SC_UNAUTHORIZED))
                                        .accessDeniedHandler(
                                                (request, response, accessDeniedException) ->
                                                        response.sendError(
                                                                HttpServletResponse.SC_FORBIDDEN)))
                .addFilterBefore(
                        jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean("permissionSecurity")
    PermissionSecurity permissionSecurity(
            ObjectProvider<AuthorizationService> authorizationServiceProvider,
            ObjectProvider<RoleAuthorizationCatalog> roleAuthorizationCatalogProvider) {
        return new PermissionSecurity(
                authorizationServiceProvider.getIfAvailable(),
                roleAuthorizationCatalogProvider.getIfAvailable(RoleAuthorizationCatalog::new));
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${mdm.security.allowed-origins:http://localhost:5273,http://127.0.0.1:5273}")
                    String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(
                Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Content-Disposition"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
