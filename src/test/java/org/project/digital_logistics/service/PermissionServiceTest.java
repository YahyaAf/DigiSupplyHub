package org.project.digital_logistics.service;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.digital_logistics.exception.AccessDeniedException;
import org.project.digital_logistics.model.User;
import org.project.digital_logistics.model.enums.Role;
import org.project.digital_logistics.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpSession session;

    @InjectMocks
    private PermissionService permissionService;

    private User adminUser;
    private User warehouseManagerUser;
    private User clientUser;

    @BeforeEach
    void setUp() {
        // Setup Admin User
        adminUser = User.builder()
                .id(1L)
                .name("admin")
                .email("admin@example.com")
                .passwordHash("encoded123")
                .role(Role.ADMIN)
                .active(true)
                .build();

        // Setup Warehouse Manager User
        warehouseManagerUser = User.builder()
                .id(2L)
                .name("warehouse_manager")
                .email("manager@example.com")
                .passwordHash("encoded456")
                .role(Role.WAREHOUSE_MANAGER)
                .active(true)
                .build();

        // Setup Client User
        clientUser = User.builder()
                .id(3L)
                .name("client")
                .email("client@example.com")
                .passwordHash("encoded789")
                .role(Role.CLIENT)
                .active(true)
                .build();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // GET AUTHENTICATED USER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getAuthenticatedUser_Success() {
        // Given
        when(session.getAttribute("authenticated_user")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        // When
        User result = permissionService.getAuthenticatedUser(session);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("admin", result.getName());
        assertEquals(Role.ADMIN, result.getRole());

        verify(session).getAttribute("authenticated_user");
        verify(userRepository).findById(1L);
    }

    @Test
    void getAuthenticatedUser_NotAuthenticated_ThrowsException() {
        // Given
        when(session.getAttribute("authenticated_user")).thenReturn(null);

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> permissionService.getAuthenticatedUser(session)
        );

        assertEquals("Not authenticated. Please login.", exception.getMessage());

        verify(session).getAttribute("authenticated_user");
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void getAuthenticatedUser_UserNotFound_ThrowsException() {
        // Given
        when(session.getAttribute("authenticated_user")).thenReturn(999L);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> permissionService.getAuthenticatedUser(session)
        );

        assertEquals("User not found", exception.getMessage());

        verify(session).getAttribute("authenticated_user");
        verify(userRepository).findById(999L);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // REQUIRE ADMIN TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void requireAdmin_AdminUser_Success() {
        // Given
        when(session.getAttribute("authenticated_user")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        // When & Then
        assertDoesNotThrow(() -> permissionService.requireAdmin(session));

        verify(session).getAttribute("authenticated_user");
        verify(userRepository).findById(1L);
    }

    @Test
    void requireAdmin_NonAdminUser_ThrowsException() {
        // Given
        when(session.getAttribute("authenticated_user")).thenReturn(3L);
        when(userRepository.findById(3L)).thenReturn(Optional.of(clientUser));

        // When & Then
        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> permissionService.requireAdmin(session)
        );

        assertEquals("Access denied. Admin privileges required.", exception.getMessage());

        verify(session).getAttribute("authenticated_user");
        verify(userRepository).findById(3L);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // REQUIRE WAREHOUSE MANAGER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void requireWarehouseManager_AdminUser_Success() {
        // Given
        when(session.getAttribute("authenticated_user")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        // When & Then
        assertDoesNotThrow(() -> permissionService.requireWarehouseManager(session));

        verify(session).getAttribute("authenticated_user");
        verify(userRepository).findById(1L);
    }

    @Test
    void requireWarehouseManager_WarehouseManagerUser_Success() {
        // Given
        when(session.getAttribute("authenticated_user")).thenReturn(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(warehouseManagerUser));

        // When & Then
        assertDoesNotThrow(() -> permissionService.requireWarehouseManager(session));

        verify(session).getAttribute("authenticated_user");
        verify(userRepository).findById(2L);
    }

    @Test
    void requireWarehouseManager_ClientUser_ThrowsException() {
        // Given
        when(session.getAttribute("authenticated_user")).thenReturn(3L);
        when(userRepository.findById(3L)).thenReturn(Optional.of(clientUser));

        // When & Then
        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> permissionService.requireWarehouseManager(session)
        );

        assertEquals(
                "Access denied. Warehouse Manager or Admin privileges required.",
                exception.getMessage()
        );

        verify(session).getAttribute("authenticated_user");
        verify(userRepository).findById(3L);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CHECK CLIENT OWNERSHIP TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void checkClientOwnership_AdminUser_AllowsAccess() {
        // Given
        Long resourceClientId = 999L; // Different from admin's ID
        when(session.getAttribute("authenticated_user")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        // When & Then
        assertDoesNotThrow(
                () -> permissionService.checkClientOwnership(session, resourceClientId)
        );

        verify(session).getAttribute("authenticated_user");
        verify(userRepository).findById(1L);
    }

    @Test
    void checkClientOwnership_WarehouseManager_AllowsAccess() {
        // Given
        Long resourceClientId = 999L; // Different from manager's ID
        when(session.getAttribute("authenticated_user")).thenReturn(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(warehouseManagerUser));

        // When & Then
        assertDoesNotThrow(
                () -> permissionService.checkClientOwnership(session, resourceClientId)
        );

        verify(session).getAttribute("authenticated_user");
        verify(userRepository).findById(2L);
    }

    @Test
    void checkClientOwnership_ClientAccessingOwnResource_Success() {
        // Given
        Long resourceClientId = 3L; // Same as client's ID
        when(session.getAttribute("authenticated_user")).thenReturn(3L);
        when(userRepository.findById(3L)).thenReturn(Optional.of(clientUser));

        // When & Then
        assertDoesNotThrow(
                () -> permissionService.checkClientOwnership(session, resourceClientId)
        );

        verify(session).getAttribute("authenticated_user");
        verify(userRepository).findById(3L);
    }

    @Test
    void checkClientOwnership_ClientAccessingOthersResource_ThrowsException() {
        // Given
        Long resourceClientId = 999L; // Different from client's ID
        when(session.getAttribute("authenticated_user")).thenReturn(3L);
        when(userRepository.findById(3L)).thenReturn(Optional.of(clientUser));

        // When & Then
        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> permissionService.checkClientOwnership(session, resourceClientId)
        );

        assertEquals(
                "Access denied. You can only access your own orders.",
                exception.getMessage()
        );

        verify(session).getAttribute("authenticated_user");
        verify(userRepository).findById(3L);
    }

    @Test
    void checkClientOwnership_ClientWithNullResourceId_ThrowsException() {
        // Given
        Long resourceClientId = null;
        when(session.getAttribute("authenticated_user")).thenReturn(3L);
        when(userRepository.findById(3L)).thenReturn(Optional.of(clientUser));

        // When & Then
        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> permissionService.checkClientOwnership(session, resourceClientId)
        );

        assertEquals(
                "Access denied. You can only access your own orders.",
                exception.getMessage()
        );

        verify(session).getAttribute("authenticated_user");
        verify(userRepository).findById(3L);
    }
}