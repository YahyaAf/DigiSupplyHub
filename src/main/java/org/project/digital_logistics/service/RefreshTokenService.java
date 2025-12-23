package org.project.digital_logistics.service;

import lombok.RequiredArgsConstructor;
import org.project.digital_logistics.config.JwtUtil;
import org.project.digital_logistics.model.RefreshToken;
import org.project.digital_logistics.model.User;
import org.project.digital_logistics.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time. LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    @Value("${jwt.refresh-token.expiration}")
    private Long refreshTokenDuration;

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        refreshTokenRepository.revokeAllUserTokens(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(LocalDateTime.now().plusSeconds(refreshTokenDuration / 1000))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token expiré.  Veuillez vous reconnecter.");
        }

        if (token.getRevoked()) {
            throw new RuntimeException("Refresh token révoqué. Veuillez vous reconnecter.");
        }

        return token;
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public void revokeUserTokens(User user) {
        refreshTokenRepository.revokeAllUserTokens(user);
    }

    @Transactional
    public void deleteExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }
}