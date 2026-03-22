package com.industrial.mdm.common.security;

import com.industrial.mdm.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_ENTERPRISE_ID = "enterpriseId";
    public static final String CLAIM_DISPLAY_NAME = "displayName";
    public static final String CLAIM_ORGANIZATION = "organization";
    public static final String CLAIM_TOKEN_TYPE = "tokenType";

    private final JwtProperties jwtProperties;
    private final Key signingKey;

    public JwtTokenService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey =
                Keys.hmacShaKeyFor(jwtProperties.jwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public TokenPair issueTokens(AuthenticatedUser user) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime accessExpiresAt = now.plusMinutes(jwtProperties.accessTokenMinutes());
        OffsetDateTime refreshExpiresAt = now.plusDays(jwtProperties.refreshTokenDays());
        String accessToken = buildToken(user, "access", accessExpiresAt);
        String refreshToken = buildToken(user, "refresh", refreshExpiresAt);
        return new TokenPair(accessToken, refreshToken, accessExpiresAt, refreshExpiresAt);
    }

    public AuthenticatedUser parseAccessToken(String token) {
        Claims claims = parseClaims(token);
        if (!"access".equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
            throw new IllegalArgumentException("invalid access token");
        }
        return buildUser(claims);
    }

    public AuthenticatedUser parseRefreshToken(String token) {
        Claims claims = parseClaims(token);
        if (!"refresh".equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
            throw new IllegalArgumentException("invalid refresh token");
        }
        return buildUser(claims);
    }

    private String buildToken(AuthenticatedUser user, String tokenType, OffsetDateTime expiresAt) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.userId().toString())
                .claim(CLAIM_ROLE, user.role().getCode())
                .claim(
                        CLAIM_ENTERPRISE_ID,
                        user.enterpriseId() == null ? null : user.enterpriseId().toString())
                .claim(CLAIM_DISPLAY_NAME, user.displayName())
                .claim(CLAIM_ORGANIZATION, user.organization())
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .issuedAt(new Date())
                .expiration(Date.from(expiresAt.toInstant()))
                .signWith(signingKey)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith((javax.crypto.SecretKey) signingKey).build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private AuthenticatedUser buildUser(Claims claims) {
        String enterpriseId = claims.get(CLAIM_ENTERPRISE_ID, String.class);
        return new AuthenticatedUser(
                UUID.fromString(claims.getSubject()),
                UserRole.fromCode(claims.get(CLAIM_ROLE, String.class)),
                enterpriseId == null || enterpriseId.isBlank()
                        ? null
                        : UUID.fromString(enterpriseId),
                claims.get(CLAIM_DISPLAY_NAME, String.class),
                claims.get(CLAIM_ORGANIZATION, String.class));
    }

    public record TokenPair(
            String accessToken,
            String refreshToken,
            OffsetDateTime accessTokenExpiresAt,
            OffsetDateTime refreshTokenExpiresAt) {}
}
