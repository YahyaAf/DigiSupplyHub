package org.project.digital_logistics.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.UserRequestDto;
import org.project.digital_logistics.dto.UserResponseDto;
import org.project.digital_logistics.exception.AccessDeniedException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.model.enums.Role;
import org.project.digital_logistics.service.PermissionService;
import org.project.digital_logistics.service.UserService;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private PermissionService permissionService;

    private MockHttpSession session;
    private UserRequestDto requestDto;
    private UserResponseDto responseDto;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();

        // Setup UserRequestDto
        requestDto = new UserRequestDto();
        requestDto.setName("john_manager");  // ✅ Changed from setUsername
        requestDto.setEmail("john.manager@example.com");
        requestDto.setPassword("SecurePassword123");
        requestDto.setRole(Role.WAREHOUSE_MANAGER);

        // Setup UserResponseDto
        responseDto = new UserResponseDto();
        responseDto.setId(1L);
        responseDto.setName("john_manager");  // ✅ Changed from setUsername
        responseDto.setEmail("john.manager@example.com");
        responseDto.setRole(Role.WAREHOUSE_MANAGER);
        responseDto.setActive(true);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CREATE USER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void createUser_AsAdmin_ReturnsCreated() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(userService.createUser(any(UserRequestDto.class)))
                .thenReturn(new ApiResponse<>("User created successfully", responseDto));

        // When & Then
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User created successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("john_manager"))  // ✅ Changed from username
                .andExpect(jsonPath("$.data.email").value("john.manager@example.com"));

        verify(permissionService).requireAdmin(any());
        verify(userService).createUser(any(UserRequestDto.class));
    }

    @Test
    void createUser_AsNonAdmin_ReturnsForbidden() throws Exception {
        // Given
        doThrow(new AccessDeniedException("Access denied. Admin privileges required."))
                .when(permissionService).requireAdmin(any());

        // When & Then
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isForbidden());

        verify(permissionService).requireAdmin(any());
        verify(userService, never()).createUser(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // GET USER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getUserById_Success_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(userService.getUserById(1L))
                .thenReturn(new ApiResponse<>("User retrieved successfully", responseDto));

        // When & Then
        mockMvc.perform(get("/api/users/1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("john_manager"));  // ✅ Changed from username

        verify(permissionService).requireAdmin(any());
        verify(userService).getUserById(1L);
    }

    @Test
    void getUserById_NotFound_ReturnsNotFound() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(userService.getUserById(999L))
                .thenThrow(new ResourceNotFoundException("User", "id", 999L));

        // When & Then
        mockMvc.perform(get("/api/users/999")
                        .session(session))
                .andExpect(status().isNotFound());

        verify(userService).getUserById(999L);
    }

    @Test
    void getUserByEmail_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(userService.getUserByEmail("john.manager@example.com"))
                .thenReturn(new ApiResponse<>("User retrieved successfully", responseDto));

        // When & Then
        mockMvc.perform(get("/api/users/email/john.manager@example.com")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("john.manager@example.com"));

        verify(userService).getUserByEmail("john.manager@example.com");
    }

    @Test
    void getAllUsers_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        List<UserResponseDto> users = Arrays.asList(responseDto);
        when(userService.getAllUsers())
                .thenReturn(new ApiResponse<>("Users retrieved successfully", users));

        // When & Then
        mockMvc.perform(get("/api/users")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Users retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("john_manager"));  // ✅ Changed from username

        verify(userService).getAllUsers();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // UPDATE USER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void updateUser_AsAdmin_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(userService.updateUser(eq(1L), any(UserRequestDto.class)))
                .thenReturn(new ApiResponse<>("User updated successfully", responseDto));

        // When & Then
        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User updated successfully"))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(permissionService).requireAdmin(any());
        verify(userService).updateUser(eq(1L), any(UserRequestDto.class));
    }

    @Test
    void updateUser_AsNonAdmin_ReturnsForbidden() throws Exception {
        // Given
        doThrow(new AccessDeniedException("Access denied"))
                .when(permissionService).requireAdmin(any());

        // When & Then
        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isForbidden());

        verify(userService, never()).updateUser(anyLong(), any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // DELETE USER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void deleteUser_AsAdmin_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(userService.deleteUser(1L))
                .thenReturn(new ApiResponse<>("User deleted successfully", responseDto));

        // When & Then
        mockMvc.perform(delete("/api/users/1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User deleted successfully"))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(permissionService).requireAdmin(any());
        verify(userService).deleteUser(1L);
    }

    @Test
    void deleteUser_AsNonAdmin_ReturnsForbidden() throws Exception {
        // Given
        doThrow(new AccessDeniedException("Access denied"))
                .when(permissionService).requireAdmin(any());

        // When & Then
        mockMvc.perform(delete("/api/users/1")
                        .session(session))
                .andExpect(status().isForbidden());

        verify(userService, never()).deleteUser(anyLong());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // COUNT TEST
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void countUsers_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(userService.countUsers())
                .thenReturn(new ApiResponse<>("Total users count", 20L));

        // When & Then
        mockMvc.perform(get("/api/users/count")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Total users count"))
                .andExpect(jsonPath("$.data").value(20));

        verify(userService).countUsers();
    }
}