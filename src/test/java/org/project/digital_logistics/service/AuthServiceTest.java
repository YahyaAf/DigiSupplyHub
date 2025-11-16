package org.project.digital_logistics.service;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.digital_logistics.dto.auth.AuthResponseDto;
import org.project.digital_logistics.dto.auth.LoginRequestDto;
import org.project.digital_logistics.dto.auth.RegisterRequestDto;
import org.project.digital_logistics.exception.DuplicateResourceException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.model.Client;
import org.project.digital_logistics.model.User;
import org.project.digital_logistics.model.enums.Role;
import org.project.digital_logistics.repository.ClientRepository;
import org.project.digital_logistics.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private HttpSession session;

    @InjectMocks
    private AuthService authService;

    private RegisterRequestDto registerDto;
    private LoginRequestDto loginDto;
    private Client client;
    private User user;

    @BeforeEach
    void setUp() {
        // ✅ Setup RegisterRequestDto (WITHOUT builder)
        registerDto = new RegisterRequestDto();
        registerDto.setName("John Doe");
        registerDto.setEmail("john.doe@example.com");
        registerDto.setPhoneNumber("0612345678");
        registerDto.setAddress("123 Main Street, Casablanca");
        registerDto.setPassword("SecurePassword123");

        // ✅ Setup LoginRequestDto (WITHOUT builder)
        loginDto = new LoginRequestDto();
        loginDto.setEmail("admin@example.com");
        loginDto.setPassword("AdminPass123");

        // Setup Client
        client = Client.builder()
                .id(1L)
                .name("John Doe")
                .email("john.doe@example.com")
                .phoneNumber("0612345678")
                .address("123 Main Street")
                .passwordHash("encodedPassword123")
                .active(true)
                .build();

        // Setup User
        user = User.builder()
                .id(2L)
                .name("admin")
                .email("admin@example.com")
                .passwordHash("encodedAdminPass")
                .role(Role.ADMIN)
                .active(true)
                .build();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // REGISTER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void register_Success() {
        // Given
        when(clientRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(clientRepository.existsByPhoneNumber("0612345678")).thenReturn(false);
        when(passwordEncoder.encode("SecurePassword123")).thenReturn("encodedPassword123");
        when(clientRepository.save(any(Client.class))).thenReturn(client);
        doNothing().when(session).setAttribute(anyString(), anyLong());

        // When
        AuthResponseDto response = authService.register(registerDto, session);

        // Then
        assertNotNull(response);
        assertEquals("Registration successful", response.getMessage());
        assertEquals("john.doe@example.com", response.getEmail());
        assertEquals("John Doe", response.getName());

        verify(clientRepository).existsByEmail("john.doe@example.com");
        verify(clientRepository).existsByPhoneNumber("0612345678");
        verify(passwordEncoder).encode("SecurePassword123");
        verify(clientRepository).save(any(Client.class));
        verify(session).setAttribute("authenticated_user", 1L);
    }

    @Test
    void register_DuplicateEmail_ThrowsException() {
        // Given
        when(clientRepository.existsByEmail("john.doe@example.com")).thenReturn(true);

        // When & Then
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> authService.register(registerDto, session)
        );

        assertTrue(exception.getMessage().contains("email"));
        assertTrue(exception.getMessage().contains("john.doe@example.com"));

        verify(clientRepository).existsByEmail("john.doe@example.com");
        verify(clientRepository, never()).existsByPhoneNumber(anyString());
        verify(clientRepository, never()).save(any());
        verify(session, never()).setAttribute(anyString(), any());
    }

    @Test
    void register_DuplicatePhoneNumber_ThrowsException() {
        // Given
        when(clientRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(clientRepository.existsByPhoneNumber("0612345678")).thenReturn(true);

        // When & Then
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> authService.register(registerDto, session)
        );

        assertTrue(exception.getMessage().contains("phoneNumber"));
        assertTrue(exception.getMessage().contains("0612345678"));

        verify(clientRepository).existsByEmail("john.doe@example.com");
        verify(clientRepository).existsByPhoneNumber("0612345678");
        verify(clientRepository, never()).save(any());
        verify(session, never()).setAttribute(anyString(), any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // LOGIN TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void login_Success() {
        // Given
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("AdminPass123", "encodedAdminPass")).thenReturn(true);
        doNothing().when(session).setAttribute(anyString(), anyLong());

        // When
        AuthResponseDto response = authService.login(loginDto, session);

        // Then
        assertNotNull(response);
        assertEquals("Login successful", response.getMessage());
        assertEquals("admin@example.com", response.getEmail());
        assertEquals("admin", response.getName());

        verify(userRepository).findByEmail("admin@example.com");
        verify(passwordEncoder).matches("AdminPass123", "encodedAdminPass");
        verify(session).setAttribute("authenticated_user", 2L);
    }

    @Test
    void login_UserNotFound_ThrowsException() {
        // Given
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());
        loginDto.setEmail("notfound@example.com");

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> authService.login(loginDto, session)
        );

        assertTrue(exception.getMessage().contains("User"));
        assertTrue(exception.getMessage().contains("email"));

        verify(userRepository).findByEmail("notfound@example.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(session, never()).setAttribute(anyString(), any());
    }

    @Test
    void login_InvalidPassword_ThrowsException() {
        // Given
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "encodedAdminPass")).thenReturn(false);
        loginDto.setPassword("WrongPassword");

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(loginDto, session)
        );

        assertEquals("Invalid email or password", exception.getMessage());

        verify(userRepository).findByEmail("admin@example.com");
        verify(passwordEncoder).matches("WrongPassword", "encodedAdminPass");
        verify(session, never()).setAttribute(anyString(), any());
    }

    @Test
    void login_InactiveAccount_ThrowsException() {
        // Given
        user.setActive(false);
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("AdminPass123", "encodedAdminPass")).thenReturn(true);

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> authService.login(loginDto, session)
        );

        assertEquals("Account is inactive. Please contact support.", exception.getMessage());

        verify(userRepository).findByEmail("admin@example.com");
        verify(passwordEncoder).matches("AdminPass123", "encodedAdminPass");
        verify(session, never()).setAttribute(anyString(), any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // LOGOUT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void logout_Success() {
        // Given
        doNothing().when(session).removeAttribute("authenticated_user");
        doNothing().when(session).invalidate();

        // When
        String message = authService.logout(session);

        // Then
        assertEquals("Logout successful", message);
        verify(session).removeAttribute("authenticated_user");
        verify(session).invalidate();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // GET CURRENT USER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getCurrentUser_Success() {
        // Given
        when(session.getAttribute("authenticated_user")).thenReturn(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        // When
        AuthResponseDto response = authService.getCurrentUser(session);

        // Then
        assertNotNull(response);
        assertEquals("User retrieved successfully", response.getMessage());
        assertEquals("admin@example.com", response.getEmail());
        assertEquals("admin", response.getName());

        verify(session).getAttribute("authenticated_user");
        verify(userRepository).findById(2L);
    }

    @Test
    void getCurrentUser_NotAuthenticated_ThrowsException() {
        // Given
        when(session.getAttribute("authenticated_user")).thenReturn(null);

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> authService.getCurrentUser(session)
        );

        assertEquals("Not authenticated. Please login.", exception.getMessage());

        verify(session).getAttribute("authenticated_user");
        verify(userRepository, never()).findById(anyLong());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // IS AUTHENTICATED TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void isAuthenticated_UserLoggedIn_ReturnsTrue() {
        // Given
        when(session.getAttribute("authenticated_user")).thenReturn(2L);

        // When
        boolean result = authService.isAuthenticated(session);

        // Then
        assertTrue(result);
        verify(session).getAttribute("authenticated_user");
    }

    @Test
    void isAuthenticated_UserNotLoggedIn_ReturnsFalse() {
        // Given
        when(session.getAttribute("authenticated_user")).thenReturn(null);

        // When
        boolean result = authService.isAuthenticated(session);

        // Then
        assertFalse(result);
        verify(session).getAttribute("authenticated_user");
    }
}