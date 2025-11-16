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
import org.project.digital_logistics.dto.ClientRequestDto;
import org.project.digital_logistics.dto.ClientResponseDto;
import org.project.digital_logistics.exception.AccessDeniedException;
import org.project.digital_logistics.exception.DuplicateResourceException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.service.ClientService;
import org.project.digital_logistics.service.PermissionService;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClientController.class)
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClientService clientService;

    @MockBean
    private PermissionService permissionService;

    private MockHttpSession session;
    private ClientRequestDto requestDto;
    private ClientResponseDto responseDto;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();

        // Setup ClientRequestDto
        requestDto = new ClientRequestDto();
        requestDto.setName("John Doe");
        requestDto.setEmail("john.doe@example.com");
        requestDto.setPhoneNumber("0612345678");
        requestDto.setAddress("123 Main Street, Casablanca");
        requestDto.setPassword("SecurePassword123");

        // Setup ClientResponseDto
        responseDto = new ClientResponseDto();
        responseDto.setId(1L);
        responseDto.setName("John Doe");
        responseDto.setEmail("john.doe@example.com");
        responseDto.setPhoneNumber("0612345678");
        responseDto.setAddress("123 Main Street, Casablanca");
        responseDto.setActive(true);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CREATE CLIENT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void createClient_AsAdmin_ReturnsCreated() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(clientService.createClient(any(ClientRequestDto.class)))
                .thenReturn(new ApiResponse<>("Client created successfully", responseDto));

        // When & Then
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Client created successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.email").value("john.doe@example.com"));

        verify(permissionService).requireAdmin(any());
        verify(clientService).createClient(any(ClientRequestDto.class));
    }

    @Test
    void createClient_AsNonAdmin_ReturnsForbidden() throws Exception {
        // Given
        doThrow(new AccessDeniedException("Access denied. Admin privileges required."))
                .when(permissionService).requireAdmin(any());

        // When & Then
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isForbidden());

        verify(permissionService).requireAdmin(any());
        verify(clientService, never()).createClient(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // GET CLIENT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getClientById_Success_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(clientService.getClientById(1L))
                .thenReturn(new ApiResponse<>("Client retrieved successfully", responseDto));

        // When & Then
        mockMvc.perform(get("/api/clients/1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Client retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.email").value("john.doe@example.com"));

        verify(permissionService).requireAdmin(any());
        verify(clientService).getClientById(1L);
    }

    @Test
    void getClientById_NotFound_ReturnsNotFound() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(clientService.getClientById(999L))
                .thenThrow(new ResourceNotFoundException("Client", "id", 999L));

        // When & Then
        mockMvc.perform(get("/api/clients/999")
                        .session(session))
                .andExpect(status().isNotFound());

        verify(clientService).getClientById(999L);
    }

    @Test
    void getClientByEmail_Success_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(clientService.getClientByEmail("john.doe@example.com"))
                .thenReturn(new ApiResponse<>("Client retrieved successfully", responseDto));

        // When & Then
        mockMvc.perform(get("/api/clients/email/john.doe@example.com")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("john.doe@example.com"));

        verify(clientService).getClientByEmail("john.doe@example.com");
    }

    @Test
    void getClientByPhoneNumber_Success_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(clientService.getClientByPhoneNumber("0612345678"))
                .thenReturn(new ApiResponse<>("Client retrieved successfully", responseDto));

        // When & Then
        mockMvc.perform(get("/api/clients/phone/0612345678")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phoneNumber").value("0612345678"));

        verify(clientService).getClientByPhoneNumber("0612345678");
    }

    @Test
    void getAllClients_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        List<ClientResponseDto> clients = Arrays.asList(responseDto);
        when(clientService.getAllClients())
                .thenReturn(new ApiResponse<>("Clients retrieved successfully", clients));

        // When & Then
        mockMvc.perform(get("/api/clients")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Clients retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].email").value("john.doe@example.com"));

        verify(clientService).getAllClients();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // UPDATE CLIENT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void updateClient_AsAdmin_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(clientService.updateClient(eq(1L), any(ClientRequestDto.class)))
                .thenReturn(new ApiResponse<>("Client updated successfully", responseDto));

        // When & Then
        mockMvc.perform(put("/api/clients/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Client updated successfully"))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(permissionService).requireAdmin(any());
        verify(clientService).updateClient(eq(1L), any(ClientRequestDto.class));
    }

    @Test
    void updateClient_AsNonAdmin_ReturnsForbidden() throws Exception {
        // Given
        doThrow(new AccessDeniedException("Access denied"))
                .when(permissionService).requireAdmin(any());

        // When & Then
        mockMvc.perform(put("/api/clients/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isForbidden());

        verify(clientService, never()).updateClient(anyLong(), any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // DELETE CLIENT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void deleteClient_AsAdmin_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(clientService.deleteClient(1L))
                .thenReturn(new ApiResponse<>("Client deleted successfully", responseDto));

        // When & Then
        mockMvc.perform(delete("/api/clients/1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Client deleted successfully"))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(permissionService).requireAdmin(any());
        verify(clientService).deleteClient(1L);
    }

    @Test
    void deleteClient_AsNonAdmin_ReturnsForbidden() throws Exception {
        // Given
        doThrow(new AccessDeniedException("Access denied"))
                .when(permissionService).requireAdmin(any());

        // When & Then
        mockMvc.perform(delete("/api/clients/1")
                        .session(session))
                .andExpect(status().isForbidden());

        verify(clientService, never()).deleteClient(anyLong());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // COUNT TEST
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void countClients_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(clientService.countClients())
                .thenReturn(new ApiResponse<>("Total clients count", 25L));

        // When & Then
        mockMvc.perform(get("/api/clients/count")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Total clients count"))
                .andExpect(jsonPath("$.data").value(25));

        verify(clientService).countClients();
    }
}