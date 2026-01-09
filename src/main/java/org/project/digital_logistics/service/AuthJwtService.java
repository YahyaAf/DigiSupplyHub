package org.project.digital_logistics. service;

import lombok.RequiredArgsConstructor;
import org.project.digital_logistics.config.JwtUtil;
import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.ClientRequestDto;
import org.project.digital_logistics.dto.authJwt.AuthResponse;
import org.project.digital_logistics.dto.authJwt.LoginRequest;
import org.project.digital_logistics.dto.authJwt.RefreshTokenRequest;
import org.project.digital_logistics.dto.authJwt.RegisterRequest;
import org.project.digital_logistics.model.RefreshToken;
import org.project.digital_logistics.model.User;
import org.project.digital_logistics.repository.UserRepository;
import org.springframework.security. authentication.AuthenticationManager;
import org.springframework.security.authentication. UsernamePasswordAuthenticationToken;
import org.springframework.security. core.Authentication;
import org. springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthJwtService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final ClientService clientService;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = (User) authentication.getPrincipal();

        if (!user.getActive()) {
            throw new IllegalStateException("Compte désactivé.  Veuillez contacter le support.");
        }

        String accessToken = jwtUtil.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiration() / 1000)
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        return refreshTokenService.findByToken(request.getRefreshToken())
                .map(refreshTokenService:: verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String accessToken = jwtUtil.generateAccessToken(user);
                    RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

                    return AuthResponse.builder()
                            .accessToken(accessToken)
                            .refreshToken(newRefreshToken.getToken())
                            .tokenType("Bearer")
                            .expiresIn(jwtUtil.getAccessTokenExpiration() / 1000)
                            .email(user. getEmail())
                            .role(user.getRole().name())
                            .build();
                })
                .orElseThrow(() -> new IllegalArgumentException("Refresh token invalide ou expiré"));
    }

    @Transactional
    public void logout(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé avec email: " + email));

        refreshTokenService.revokeUserTokens(user);
    }

    @Transactional
    public ApiResponse<String> register(RegisterRequest request) {
        ClientRequestDto clientRequestDto = ClientRequestDto.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(request.getPassword())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .active(true)
                .build();

        clientService.createClient(clientRequestDto);

        return ApiResponse.<String>builder()
                .success(true)
                .message("Inscription réussie. Veuillez vous connecter.")
                .data(null)
                .build();
    }
}