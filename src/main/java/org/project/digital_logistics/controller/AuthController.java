package org.project.digital_logistics.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.auth.AuthResponseDto;
import org.project.digital_logistics.dto.auth.LoginRequestDto;
import org.project.digital_logistics.dto.auth.RegisterRequestDto;
import org.project.digital_logistics.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Register new Client
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponseDto>> register(
            @Valid @RequestBody RegisterRequestDto registerDto,
            HttpSession session) {

        AuthResponseDto response = authService.register(registerDto, session);
        ApiResponse<AuthResponseDto> apiResponse = new ApiResponse<>(
                "Registration successful. You are now logged in.",
                response
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    /**
     * Login (All users: User, Client, Admin, etc.)
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDto>> login(
            @Valid @RequestBody LoginRequestDto loginDto,
            HttpSession session) {

        AuthResponseDto response = authService.login(loginDto, session);
        ApiResponse<AuthResponseDto> apiResponse = new ApiResponse<>(
                "Login successful",
                response
        );
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Logout
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpSession session) {
        String message = authService.logout(session);
        ApiResponse<String> apiResponse = new ApiResponse<>(message, null);
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Get current authenticated user
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponseDto>> getCurrentUser(HttpSession session) {
        AuthResponseDto response = authService.getCurrentUser(session);
        ApiResponse<AuthResponseDto> apiResponse = new ApiResponse<>(
                "User retrieved successfully",
                response
        );
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Check authentication status
     * GET /api/auth/status
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Boolean>> checkAuthStatus(HttpSession session) {
        boolean isAuthenticated = authService.isAuthenticated(session);
        ApiResponse<Boolean> apiResponse = new ApiResponse<>(
                isAuthenticated ? "User is authenticated" : "User is not authenticated",
                isAuthenticated
        );
        return ResponseEntity.ok(apiResponse);
    }
}