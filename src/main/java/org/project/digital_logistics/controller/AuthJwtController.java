package org. project.digital_logistics.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.digital_logistics.dto.ApiResponse;
import org.project. digital_logistics.dto.authJwt.AuthResponse;
import org.project.digital_logistics.dto.authJwt.LoginRequest;
import org.project.digital_logistics.dto.authJwt.RefreshTokenRequest;
import org.project.digital_logistics.service.AuthJwtService;
import org.springframework.http.ResponseEntity;
import org.springframework. security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/jwt")
@RequiredArgsConstructor
public class AuthJwtController {

    private final AuthJwtService authJwtService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = authJwtService.login(request);
        ApiResponse<AuthResponse> response = new ApiResponse<>("Connexion réussie", authResponse);
        return ResponseEntity. ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse authResponse = authJwtService.refreshToken(request);
        ApiResponse<AuthResponse> response = new ApiResponse<>("Token renouvelé avec succès", authResponse);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            authJwtService.logout(email);
            ApiResponse<Void> response = new ApiResponse<>("Déconnexion réussie", null);
            return ResponseEntity.ok(response);
        }
        ApiResponse<Void> response = new ApiResponse<>("Aucun utilisateur connecté", null);
        response.setSuccess(false);
        return ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<? >> getCurrentUser(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            ApiResponse<? > response = new ApiResponse<>("Utilisateur authentifié", authentication.getPrincipal());
            return ResponseEntity.ok(response);
        }
        ApiResponse<Void> response = new ApiResponse<>("Non authentifié", null);
        response.setSuccess(false);
        return ResponseEntity.status(401).body(response);
    }
}