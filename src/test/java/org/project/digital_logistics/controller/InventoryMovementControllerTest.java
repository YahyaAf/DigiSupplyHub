package org.project.digital_logistics.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.inventorymovement.InventoryMovementResponseDto;
import org.project.digital_logistics.exception.AccessDeniedException;
import org.project.digital_logistics.model.enums.MovementType;
import org.project.digital_logistics.service.InventoryMovementService;
import org.project.digital_logistics.service.PermissionService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryMovementController.class)
class InventoryMovementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryMovementService movementService;

    @MockBean
    private PermissionService permissionService;

    private MockHttpSession session;
    private InventoryMovementResponseDto responseDto;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();

        // Setup InventoryMovementResponseDto
        responseDto = new InventoryMovementResponseDto();
        responseDto.setId(1L);
        responseDto.setInventoryId(1L);
        responseDto.setType(MovementType.INBOUND);
        responseDto.setQuantity(50);
        responseDto.setReferenceDocument("PO-123");
        responseDto.setDescription("Purchase order reception");
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // GET ALL MOVEMENTS TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getAllMovements_AsWarehouseManager_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        List<InventoryMovementResponseDto> movements = Arrays.asList(responseDto);
        when(movementService.getAllMovements())
                .thenReturn(new ApiResponse<>("Inventory movements retrieved successfully", movements));

        // When & Then
        mockMvc.perform(get("/api/inventory-movements")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Inventory movements retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].type").value("INBOUND"))
                .andExpect(jsonPath("$.data[0].quantity").value(50));

        verify(permissionService).requireWarehouseManager(any());
        verify(movementService).getAllMovements();
    }

    @Test
    void getAllMovements_AsClient_ReturnsForbidden() throws Exception {
        // Given
        doThrow(new AccessDeniedException("Access denied. Warehouse Manager or Admin privileges required."))
                .when(permissionService).requireWarehouseManager(any());

        // When & Then
        mockMvc.perform(get("/api/inventory-movements")
                        .session(session))
                .andExpect(status().isForbidden());

        verify(permissionService).requireWarehouseManager(any());
        verify(movementService, never()).getAllMovements();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // GET MOVEMENTS BY INVENTORY TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getMovementsByInventory_Success_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        List<InventoryMovementResponseDto> movements = Arrays.asList(responseDto);
        when(movementService.getMovementsByInventory(1L))
                .thenReturn(new ApiResponse<>("Inventory movements retrieved successfully", movements));

        // When & Then
        mockMvc.perform(get("/api/inventory-movements/inventory/1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Inventory movements retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].inventoryId").value(1))
                .andExpect(jsonPath("$.data[0].referenceDocument").value("PO-123"));

        verify(permissionService).requireWarehouseManager(any());
        verify(movementService).getMovementsByInventory(1L);
    }

    @Test
    void getMovementsByInventory_AsClient_ReturnsForbidden() throws Exception {
        // Given
        doThrow(new AccessDeniedException("Access denied"))
                .when(permissionService).requireWarehouseManager(any());

        // When & Then
        mockMvc.perform(get("/api/inventory-movements/inventory/1")
                        .session(session))
                .andExpect(status().isForbidden());

        verify(permissionService).requireWarehouseManager(any());
        verify(movementService, never()).getMovementsByInventory(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // GET MOVEMENTS BY WAREHOUSE TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getMovementsByWarehouse_Success_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        List<InventoryMovementResponseDto> movements = Arrays.asList(responseDto);
        when(movementService.getMovementsByWarehouse(1L))
                .thenReturn(new ApiResponse<>("Warehouse movements retrieved successfully", movements));

        // When & Then
        mockMvc.perform(get("/api/inventory-movements/warehouse/1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Warehouse movements retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].type").value("INBOUND"));

        verify(permissionService).requireWarehouseManager(any());
        verify(movementService).getMovementsByWarehouse(1L);
    }

    @Test
    void getMovementsByWarehouse_AsClient_ReturnsForbidden() throws Exception {
        // Given
        doThrow(new AccessDeniedException("Access denied"))
                .when(permissionService).requireWarehouseManager(any());

        // When & Then
        mockMvc.perform(get("/api/inventory-movements/warehouse/1")
                        .session(session))
                .andExpect(status().isForbidden());

        verify(permissionService).requireWarehouseManager(any());
        verify(movementService, never()).getMovementsByWarehouse(any());
    }
}