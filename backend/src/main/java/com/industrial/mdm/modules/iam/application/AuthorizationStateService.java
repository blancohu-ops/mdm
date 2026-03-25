package com.industrial.mdm.modules.iam.application;

import com.industrial.mdm.modules.auth.repository.RefreshTokenEntity;
import com.industrial.mdm.modules.auth.repository.RefreshTokenRepository;
import com.industrial.mdm.modules.auth.repository.UserEntity;
import com.industrial.mdm.modules.auth.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthorizationStateService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthorizationStateService(
            UserRepository userRepository, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public void invalidateUserAuthorization(UUID userId) {
        userRepository.findById(userId).ifPresent(this::invalidateUserAuthorization);
    }

    @Transactional
    public void invalidateUserAuthorization(UserEntity user) {
        int currentVersion = user.getAuthzVersion() == null ? 0 : user.getAuthzVersion();
        user.setAuthzVersion(currentVersion + 1);
        userRepository.save(user);

        OffsetDateTime now = OffsetDateTime.now();
        List<RefreshTokenEntity> activeRefreshTokens =
                refreshTokenRepository.findByUserIdAndExpiresAtAfterAndRevokedAtIsNull(
                        user.getId(), now);
        for (RefreshTokenEntity refreshToken : activeRefreshTokens) {
            refreshToken.setRevokedAt(now);
        }
        refreshTokenRepository.saveAll(activeRefreshTokens);
    }
}
