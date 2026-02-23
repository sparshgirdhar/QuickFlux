package com.quickflux.auth.service;

import com.quickflux.auth.domain.RefreshToken;
import com.quickflux.auth.domain.User;
import com.quickflux.auth.repository.RefreshTokenRepository;
import com.quickflux.auth.repository.UserRepository;
import com.quickflux.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse login(String email, String password) {
        log.info("Login attempt for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        // Save refresh token
        saveRefreshToken(user.getId(), refreshToken);

        log.info("Login successful for user: {}", user.getEmail());

        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getEmail(),
                user.getName()
        );
    }

    @Transactional
    public AuthResponse refreshAccessToken(String refreshToken) {
        log.info("Refresh token request");

        if (!jwtService.isTokenValid(refreshToken)) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (storedToken.getRevoked() || storedToken.isExpired()) {
            throw new InvalidTokenException("Refresh token expired or revoked");
        }

        UUID userId = jwtService.extractUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());

        log.info("Access token refreshed for user: {}", user.getEmail());

        return new AuthResponse(
                newAccessToken,
                refreshToken,
                user.getId(),
                user.getEmail(),
                user.getName()
        );
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            log.info("User logged out, refresh token revoked");
        });
    }

    private void saveRefreshToken(UUID userId, String token) {
        RefreshToken refreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .token(token)
                .expiresAt(Instant.now().plusMillis(604800000))  // 7 days
                .createdAt(Instant.now())
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
    }

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            UUID userId,
            String email,
            String name
    ) {}
}