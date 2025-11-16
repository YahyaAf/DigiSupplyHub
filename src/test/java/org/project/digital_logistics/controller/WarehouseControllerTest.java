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
import org.project.digital_logistics.dto.warehouse.WarehouseRequestDto;
import org.project.digital_logistics.dto.warehouse.WarehouseResponseDto;
import org.project.digital_logistics.exception.AccessDeniedException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.service.PermissionService;
import org.project.digital_logistics.service.WarehouseService;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WarehouseController.class)
class WarehouseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WarehouseService warehouseService;

    @MockBean
    private PermissionService permissionService;

    private MockHttpSession session;
    private WarehouseRequestDto requestDto;
    private WarehouseResponseDto responseDto;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();

        // Setup WarehouseRequestDto
        requestDto = new WarehouseRequestDto();
        requestDto.setCode("WH001");  // ✅ Changed from "WH-001" (no special characters)
        requestDto.setName("Central Warehouse");
        requestDto.setCapacity(10000);
        requestDto.setManagerId(1L);  // ✅ Added managerId (required field)

        // Setup WarehouseResponseDto
        responseDto = new WarehouseResponseDto();
        responseDto.setId(1L);
        responseDto.setCode("WH001");  // ✅ Changed from "WH-001"
        responseDto.setName("Central Warehouse");
        responseDto.setCapacity(10000);
        responseDto.setManagerId(1L);  // ✅ Added managerId
        responseDto.setActive(true);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CREATE WAREHOUSE TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void createWarehouse_AsAdmin_ReturnsCreated() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(warehouseService.createWarehouse(any(WarehouseRequestDto.class)))
                .thenReturn(new ApiResponse<>("Warehouse created successfully", responseDto));

        // When & Then
        mockMvc.perform(post("/api/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Warehouse created successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.code").value("WH001"))
                .andExpect(jsonPath("$.data.name").value("Central Warehouse"));

        verify(permissionService).requireAdmin(any());
        verify(warehouseService).createWarehouse(any(WarehouseRequestDto.class));
    }

    @Test
    void createWarehouse_AsNonAdmin_ReturnsForbidden() throws Exception {
        // Given
        doThrow(new AccessDeniedException("Access denied. Admin privileges required."))
                .when(permissionService).requireAdmin(any());

        // When & Then
        mockMvc.perform(post("/api/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isForbidden());

        verify(permissionService).requireAdmin(any());
        verify(warehouseService, never()).createWarehouse(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // GET WAREHOUSE TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getWarehouseById_Success_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(warehouseService.getWarehouseById(1L))
                .thenReturn(new ApiResponse<>("Warehouse retrieved successfully", responseDto));

        // When & Then
        mockMvc.perform(get("/api/warehouses/1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Warehouse retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.code").value("WH001"));

        verify(permissionService).requireAdmin(any());
        verify(warehouseService).getWarehouseById(1L);
    }

    @Test
    void getWarehouseById_NotFound_ReturnsNotFound() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(warehouseService.getWarehouseById(999L))
                .thenThrow(new ResourceNotFoundException("Warehouse", "id", 999L));

        // When & Then
        mockMvc.perform(get("/api/warehouses/999")
                        .session(session))
                .andExpect(status().isNotFound());

        verify(warehouseService).getWarehouseById(999L);
    }

    @Test
    void getWarehouseByCode_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(warehouseService.getWarehouseByCode("WH001"))
                .thenReturn(new ApiResponse<>("Warehouse retrieved successfully", responseDto));

        // When & Then
        mockMvc.perform(get("/api/warehouses/code/WH001")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("WH001"))
                .andExpect(jsonPath("$.data.name").value("Central Warehouse"));

        verify(warehouseService).getWarehouseByCode("WH001");
    }

    @Test
    void getAllWarehouses_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        List<WarehouseResponseDto> warehouses = Arrays.asList(responseDto);
        when(warehouseService.getAllWarehouses())
                .thenReturn(new ApiResponse<>("Warehouses retrieved successfully", warehouses));

        // When & Then
        mockMvc.perform(get("/api/warehouses")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Warehouses retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].code").value("WH001"));

        verify(warehouseService).getAllWarehouses();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // UPDATE WAREHOUSE TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void updateWarehouse_AsAdmin_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(warehouseService.updateWarehouse(eq(1L), any(WarehouseRequestDto.class)))
                .thenReturn(new ApiResponse<>("Warehouse updated successfully", responseDto));

        // When & Then
        mockMvc.perform(put("/api/warehouses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Warehouse updated successfully"))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(permissionService).requireAdmin(any());
        verify(warehouseService).updateWarehouse(eq(1L), any(WarehouseRequestDto.class));
    }

    @Test
    void updateWarehouse_AsNonAdmin_ReturnsForbidden() throws Exception {
        // Given
        doThrow(new AccessDeniedException("Access denied"))
                .when(permissionService).requireAdmin(any());

        // When & Then
        mockMvc.perform(put("/api/warehouses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isForbidden());

        verify(warehouseService, never()).updateWarehouse(anyLong(), any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // DELETE WAREHOUSE TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void deleteWarehouse_AsAdmin_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(warehouseService.deleteWarehouse(1L))
                .thenReturn(new ApiResponse<>("Warehouse deleted successfully", null));

        // When & Then
        mockMvc.perform(delete("/api/warehouses/1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Warehouse deleted successfully"));

        verify(permissionService).requireAdmin(any());
        verify(warehouseService).deleteWarehouse(1L);
    }

    @Test
    void deleteWarehouse_AsNonAdmin_ReturnsForbidden() throws Exception {
        // Given
        doThrow(new AccessDeniedException("Access denied"))
                .when(permissionService).requireAdmin(any());

        // When & Then
        mockMvc.perform(delete("/api/warehouses/1")
                        .session(session))
                .andExpect(status().isForbidden());

        verify(warehouseService, never()).deleteWarehouse(anyLong());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // COUNT TEST
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void countWarehouses_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(warehouseService.countWarehouses())
                .thenReturn(new ApiResponse<>("Total warehouses counted successfully", 5L));

        // When & Then
        mockMvc.perform(get("/api/warehouses/count")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Total warehouses counted successfully"))
                .andExpect(jsonPath("$.data").value(5));

        verify(warehouseService).countWarehouses();
    }
}