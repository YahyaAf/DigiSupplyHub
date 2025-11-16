package org.project.digital_logistics.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.project.digital_logistics.dto.auth.AuthResponseDto;
import org.project.digital_logistics.dto.auth.LoginRequestDto;
import org.project.digital_logistics.dto.auth.RegisterRequestDto;
import org.project.digital_logistics.exception.DuplicateResourceException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.service.AuthService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    private RegisterRequestDto registerDto;
    private LoginRequestDto loginDto;
    private AuthResponseDto authResponseDto;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        // Setup session
        session = new MockHttpSession();

        // Setup RegisterRequestDto
        registerDto = new RegisterRequestDto();
        registerDto.setName("John Doe");
        registerDto.setEmail("john.doe@example.com");
        registerDto.setPhoneNumber("0612345678");
        registerDto.setAddress("123 Main Street");
        registerDto.setPassword("SecurePassword123");

        // Setup LoginRequestDto
        loginDto = new LoginRequestDto();
        loginDto.setEmail("admin@example.com");
        loginDto.setPassword("AdminPass123");

        // Setup AuthResponseDto
        authResponseDto = new AuthResponseDto();
        authResponseDto.setId(1L);
        authResponseDto.setName("John Doe");
        authResponseDto.setEmail("john.doe@example.com");
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // REGISTER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void register_ValidRequest_ReturnsCreated() throws Exception {
        // Given
        when(authService.register(any(RegisterRequestDto.class), any(HttpSession.class)))
                .thenReturn(authResponseDto);

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDto))
                        .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Registration successful. You are now logged in."))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.email").value("john.doe@example.com"));

        verify(authService).register(any(RegisterRequestDto.class), any(HttpSession.class));
    }

    @Test
    void register_DuplicateEmail_ReturnsConflict() throws Exception {
        // Given
        when(authService.register(any(RegisterRequestDto.class), any(HttpSession.class)))
                .thenThrow(new DuplicateResourceException("Client", "email", "john.doe@example.com"));

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDto))
                        .session(session))
                .andExpect(status().isConflict()) // ✅ Changed from isBadRequest() to isConflict()
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());

        verify(authService).register(any(RegisterRequestDto.class), any(HttpSession.class));
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // LOGIN TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void login_ValidCredentials_ReturnsOk() throws Exception {
        // Given
        when(authService.login(any(LoginRequestDto.class), any(HttpSession.class)))
                .thenReturn(authResponseDto);

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.email").value("john.doe@example.com"));

        verify(authService).login(any(LoginRequestDto.class), any(HttpSession.class));
    }

    @Test
    void login_InvalidCredentials_ReturnsNotFound() throws Exception {
        // Given
        when(authService.login(any(LoginRequestDto.class), any(HttpSession.class)))
                .thenThrow(new ResourceNotFoundException("User", "email", "admin@example.com"));

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto))
                        .session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        verify(authService).login(any(LoginRequestDto.class), any(HttpSession.class));
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // LOGOUT TEST
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void logout_ReturnsOk() throws Exception {
        // Given
        when(authService.logout(any(HttpSession.class))).thenReturn("Logout successful");

        // When & Then
        mockMvc.perform(post("/api/auth/logout")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"));

        verify(authService).logout(any(HttpSession.class));
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // GET CURRENT USER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getCurrentUser_Authenticated_ReturnsOk() throws Exception {
        // Given
        when(authService.getCurrentUser(any(HttpSession.class))).thenReturn(authResponseDto);

        // When & Then
        mockMvc.perform(get("/api/auth/me")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.email").value("john.doe@example.com"));

        verify(authService).getCurrentUser(any(HttpSession.class));
    }

    @Test
    void getCurrentUser_NotAuthenticated_ReturnsUnauthorized() throws Exception {
        // Given
        when(authService.getCurrentUser(any(HttpSession.class)))
                .thenThrow(new IllegalStateException("Not authenticated. Please login."));

        // When & Then
        mockMvc.perform(get("/api/auth/me")
                        .session(session))
                .andExpect(status().isUnauthorized()) // ✅ Changed from isBadRequest() to isUnauthorized()
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Not authenticated. Please login."));

        verify(authService).getCurrentUser(any(HttpSession.class));
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CHECK AUTH STATUS TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void checkAuthStatus_Authenticated_ReturnsTrue() throws Exception {
        // Given
        when(authService.isAuthenticated(any(HttpSession.class))).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/auth/status")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User is authenticated"))
                .andExpect(jsonPath("$.data").value(true));

        verify(authService).isAuthenticated(any(HttpSession.class));
    }

    @Test
    void checkAuthStatus_NotAuthenticated_ReturnsFalse() throws Exception {
        // Given
        when(authService.isAuthenticated(any(HttpSession.class))).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/auth/status")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User is not authenticated"))
                .andExpect(jsonPath("$.data").value(false));

        verify(authService).isAuthenticated(any(HttpSession.class));
    }
}