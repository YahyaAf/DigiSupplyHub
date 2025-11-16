package org.project.digital_logistics.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.UserRequestDto;
import org.project.digital_logistics.dto.UserResponseDto;
import org.project.digital_logistics.exception.DuplicateResourceException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.model.User;
import org.project.digital_logistics.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserRequestDto requestDto;

    @BeforeEach
    void setUp() {
        // Setup UserRequestDto
        requestDto = UserRequestDto.builder()
                .name("testuser")
                .email("testuser@example.com")
                .password("SecurePassword123")
                .build();

        // Setup User Entity
        user = User.builder()
                .id(1L)
                .name("testuser")
                .email("testuser@example.com")
                .passwordHash("encodedPassword123")
                .active(true)
                .build();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CREATE USER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void createUser_Success() {
        // Given
        when(userRepository.existsByEmail("testuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePassword123")).thenReturn("encodedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        ApiResponse<UserResponseDto> response = userService.createUser(requestDto);

        // Then
        assertNotNull(response);
        assertEquals("User created successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals("testuser@example.com", response.getData().getEmail());
        assertEquals("testuser", response.getData().getName());

        verify(userRepository).existsByEmail("testuser@example.com");
        verify(passwordEncoder).encode("SecurePassword123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_DuplicateEmail_ThrowsException() {
        // Given
        when(userRepository.existsByEmail("testuser@example.com")).thenReturn(true);

        // When & Then
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> userService.createUser(requestDto)
        );

        assertTrue(exception.getMessage().contains("email"));
        assertTrue(exception.getMessage().contains("testuser@example.com"));

        verify(userRepository).existsByEmail("testuser@example.com");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // GET USER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getUserById_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // When
        ApiResponse<UserResponseDto> response = userService.getUserById(1L);

        // Then
        assertNotNull(response);
        assertEquals("User retrieved successfully", response.getMessage());
        assertEquals(1L, response.getData().getId());
        assertEquals("testuser@example.com", response.getData().getEmail());

        verify(userRepository).findById(1L);
    }

    @Test
    void getUserById_NotFound_ThrowsException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getUserById(999L)
        );

        assertTrue(exception.getMessage().contains("User"));
        assertTrue(exception.getMessage().contains("id"));

        verify(userRepository).findById(999L);
    }

    @Test
    void getUserByEmail_Success() {
        // Given
        when(userRepository.findByEmail("testuser@example.com"))
                .thenReturn(Optional.of(user));

        // When
        ApiResponse<UserResponseDto> response =
                userService.getUserByEmail("testuser@example.com");

        // Then
        assertNotNull(response);
        assertEquals("User retrieved successfully", response.getMessage());
        assertEquals("testuser@example.com", response.getData().getEmail());

        verify(userRepository).findByEmail("testuser@example.com");
    }

    @Test
    void getUserByEmail_NotFound_ThrowsException() {
        // Given
        when(userRepository.findByEmail("notfound@example.com"))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getUserByEmail("notfound@example.com")
        );

        assertTrue(exception.getMessage().contains("User"));
        assertTrue(exception.getMessage().contains("email"));

        verify(userRepository).findByEmail("notfound@example.com");
    }

    @Test
    void getAllUsers_Success() {
        // Given
        User user2 = User.builder()
                .id(2L)
                .name("user2")
                .email("user2@example.com")
                .passwordHash("encoded456")
                .active(true)
                .build();

        List<User> users = Arrays.asList(user, user2);
        when(userRepository.findAll()).thenReturn(users);

        // When
        ApiResponse<List<UserResponseDto>> response = userService.getAllUsers();

        // Then
        assertNotNull(response);
        assertEquals("Users retrieved successfully", response.getMessage());
        assertEquals(2, response.getData().size());
        assertEquals("testuser@example.com", response.getData().get(0).getEmail());
        assertEquals("user2@example.com", response.getData().get(1).getEmail());

        verify(userRepository).findAll();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // UPDATE USER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void updateUser_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("SecurePassword123")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        ApiResponse<UserResponseDto> response = userService.updateUser(1L, requestDto);

        // Then
        assertNotNull(response);
        assertEquals("User updated successfully", response.getMessage());

        verify(userRepository).findById(1L);
        verify(passwordEncoder).encode("SecurePassword123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_NotFound_ThrowsException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.updateUser(999L, requestDto)
        );

        assertTrue(exception.getMessage().contains("User"));

        verify(userRepository).findById(999L);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_DuplicateEmail_ThrowsException() {
        // Given
        requestDto.setEmail("newemail@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("newemail@example.com")).thenReturn(true);

        // When & Then
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> userService.updateUser(1L, requestDto)
        );

        assertTrue(exception.getMessage().contains("email"));

        verify(userRepository).findById(1L);
        verify(userRepository).existsByEmail("newemail@example.com");
        verify(userRepository, never()).save(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // DELETE USER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void deleteUser_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        doNothing().when(userRepository).deleteById(1L);

        // When
        ApiResponse<UserResponseDto> response = userService.deleteUser(1L);

        // Then
        assertNotNull(response);
        assertEquals("User deleted successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(1L, response.getData().getId());

        verify(userRepository).findById(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_NotFound_ThrowsException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.deleteUser(999L)
        );

        assertTrue(exception.getMessage().contains("User"));

        verify(userRepository).findById(999L);
        verify(userRepository, never()).deleteById(anyLong());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // COUNT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void countUsers_Success() {
        // Given
        when(userRepository.count()).thenReturn(42L);

        // When
        ApiResponse<Long> response = userService.countUsers();

        // Then
        assertNotNull(response);
        assertEquals("Total users count", response.getMessage());
        assertEquals(42L, response.getData());

        verify(userRepository).count();
    }
}