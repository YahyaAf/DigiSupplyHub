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
import org.project.digital_logistics.dto.supplier.SupplierRequestDto;
import org.project.digital_logistics.dto.supplier.SupplierResponseDto;
import org.project.digital_logistics.exception.AccessDeniedException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.service.PermissionService;
import org.project.digital_logistics.service.SupplierService;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SupplierController.class)
class SupplierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SupplierService supplierService;

    @MockBean
    private PermissionService permissionService;

    private MockHttpSession session;
    private SupplierRequestDto requestDto;
    private SupplierResponseDto responseDto;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();

        // Setup SupplierRequestDto
        requestDto = new SupplierRequestDto();
        requestDto.setName("Acme Supplies");
        requestDto.setMatricule("SUP-001");
        requestDto.setPhoneNumber("0612345678");
        requestDto.setAddress("123 Supplier Street, Casablanca");

        // Setup SupplierResponseDto
        responseDto = new SupplierResponseDto();
        responseDto.setId(1L);
        responseDto.setName("Acme Supplies");
        responseDto.setMatricule("SUP-001");
        responseDto.setPhoneNumber("0612345678");
        responseDto.setAddress("123 Supplier Street, Casablanca");
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CREATE SUPPLIER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void createSupplier_AsAdmin_ReturnsCreated() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(supplierService.createSupplier(any(SupplierRequestDto.class)))
                .thenReturn(new ApiResponse<>("Supplier created successfully", responseDto));

        // When & Then
        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Supplier created successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.matricule").value("SUP-001"));

        verify(permissionService).requireAdmin(any());
        verify(supplierService).createSupplier(any(SupplierRequestDto.class));
    }

    @Test
    void createSupplier_AsNonAdmin_ReturnsForbidden() throws Exception {
        // Given
        doThrow(new AccessDeniedException("Access denied. Admin privileges required."))
                .when(permissionService).requireAdmin(any());

        // When & Then
        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isForbidden());

        verify(permissionService).requireAdmin(any());
        verify(supplierService, never()).createSupplier(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // GET SUPPLIER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getSupplierById_Success_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(supplierService.getSupplierById(1L))
                .thenReturn(new ApiResponse<>("Supplier retrieved successfully", responseDto));

        // When & Then
        mockMvc.perform(get("/api/suppliers/1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Supplier retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Acme Supplies"));

        verify(permissionService).requireAdmin(any());
        verify(supplierService).getSupplierById(1L);
    }

    @Test
    void getSupplierById_NotFound_ReturnsNotFound() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(supplierService.getSupplierById(999L))
                .thenThrow(new ResourceNotFoundException("Supplier", "id", 999L));

        // When & Then
        mockMvc.perform(get("/api/suppliers/999")
                        .session(session))
                .andExpect(status().isNotFound());

        verify(supplierService).getSupplierById(999L);
    }

    @Test
    void getAllSuppliers_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        List<SupplierResponseDto> suppliers = Arrays.asList(responseDto);
        when(supplierService.getAllSuppliers())
                .thenReturn(new ApiResponse<>("Suppliers retrieved successfully", suppliers));

        // When & Then
        mockMvc.perform(get("/api/suppliers")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Suppliers retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("Acme Supplies"));

        verify(supplierService).getAllSuppliers();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // UPDATE SUPPLIER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void updateSupplier_AsAdmin_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(supplierService.updateSupplier(eq(1L), any(SupplierRequestDto.class)))
                .thenReturn(new ApiResponse<>("Supplier updated successfully", responseDto));

        // When & Then
        mockMvc.perform(put("/api/suppliers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Supplier updated successfully"))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(permissionService).requireAdmin(any());
        verify(supplierService).updateSupplier(eq(1L), any(SupplierRequestDto.class));
    }

    @Test
    void updateSupplier_AsNonAdmin_ReturnsForbidden() throws Exception {
        // Given
        doThrow(new AccessDeniedException("Access denied"))
                .when(permissionService).requireAdmin(any());

        // When & Then
        mockMvc.perform(put("/api/suppliers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isForbidden());

        verify(supplierService, never()).updateSupplier(anyLong(), any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // DELETE SUPPLIER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void deleteSupplier_AsAdmin_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(supplierService.deleteSupplier(1L))
                .thenReturn(new ApiResponse<>("Supplier deleted successfully", null));

        // When & Then
        mockMvc.perform(delete("/api/suppliers/1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Supplier deleted successfully"));

        verify(permissionService).requireAdmin(any());
        verify(supplierService).deleteSupplier(1L);
    }

    @Test
    void deleteSupplier_AsNonAdmin_ReturnsForbidden() throws Exception {
        // Given
        doThrow(new AccessDeniedException("Access denied"))
                .when(permissionService).requireAdmin(any());

        // When & Then
        mockMvc.perform(delete("/api/suppliers/1")
                        .session(session))
                .andExpect(status().isForbidden());

        verify(supplierService, never()).deleteSupplier(anyLong());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // COUNT TEST
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void countSuppliers_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(supplierService.countSuppliers())
                .thenReturn(new ApiResponse<>("Total suppliers counted successfully", 15L));

        // When & Then
        mockMvc.perform(get("/api/suppliers/count")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Total suppliers counted successfully"))
                .andExpect(jsonPath("$.data").value(15));

        verify(supplierService).countSuppliers();
    }
}