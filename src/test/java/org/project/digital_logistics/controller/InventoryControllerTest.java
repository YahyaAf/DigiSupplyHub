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
import org.project.digital_logistics.dto.inventory.InventoryRequestDto;
import org.project.digital_logistics.dto.inventory.InventoryResponseDto;
import org.project.digital_logistics.exception.AccessDeniedException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.service.InventoryService;
import org.project.digital_logistics.service.PermissionService;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InventoryService inventoryService;

    @MockBean
    private PermissionService permissionService;

    private MockHttpSession session;
    private InventoryRequestDto requestDto;
    private InventoryResponseDto responseDto;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();

        // Setup InventoryRequestDto
        requestDto = new InventoryRequestDto();
        requestDto.setWarehouseId(1L);
        requestDto.setProductId(1L);
        requestDto.setQtyOnHand(100);
        requestDto.setQtyReserved(10);

        // Setup InventoryResponseDto
        responseDto = new InventoryResponseDto();
        responseDto.setId(1L);
        responseDto.setWarehouseId(1L);
        responseDto.setProductId(1L);
        responseDto.setQtyOnHand(100);
        responseDto.setQtyReserved(10);
        responseDto.setQtyAvailable(90);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CREATE INVENTORY TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void createInventory_AsAdmin_ReturnsCreated() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(inventoryService.createInventory(any(InventoryRequestDto.class)))
                .thenReturn(new ApiResponse<>("Inventory created successfully", responseDto));

        // When & Then
        mockMvc.perform(post("/api/inventories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Inventory created successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.qtyOnHand").value(100));

        verify(permissionService).requireAdmin(any());
        verify(inventoryService).createInventory(any(InventoryRequestDto.class));
    }

    @Test
    void createInventory_AsNonAdmin_ReturnsForbidden() throws Exception {
        // Given
        doThrow(new AccessDeniedException("Access denied"))
                .when(permissionService).requireAdmin(any());

        // When & Then
        mockMvc.perform(post("/api/inventories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isForbidden());

        verify(inventoryService, never()).createInventory(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // GET INVENTORY TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getInventoryById_Success_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(inventoryService.getInventoryById(1L))
                .thenReturn(new ApiResponse<>("Inventory retrieved successfully", responseDto));

        // When & Then
        mockMvc.perform(get("/api/inventories/1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.qtyOnHand").value(100));

        verify(inventoryService).getInventoryById(1L);
    }

    @Test
    void getInventoryById_NotFound_ReturnsNotFound() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(inventoryService.getInventoryById(999L))
                .thenThrow(new ResourceNotFoundException("Inventory", "id", 999L));

        // When & Then
        mockMvc.perform(get("/api/inventories/999")
                        .session(session))
                .andExpect(status().isNotFound());

        verify(inventoryService).getInventoryById(999L);
    }

    @Test
    void getInventoryByWarehouseAndProduct_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(inventoryService.getInventoryByWarehouseAndProduct(1L, 1L))
                .thenReturn(new ApiResponse<>("Inventory retrieved", responseDto));

        // When & Then
        mockMvc.perform(get("/api/inventories/warehouse/1/product/1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.warehouseId").value(1))
                .andExpect(jsonPath("$.data.productId").value(1));

        verify(inventoryService).getInventoryByWarehouseAndProduct(1L, 1L);
    }

    @Test
    void getAllInventories_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(inventoryService.getAllInventories())
                .thenReturn(new ApiResponse<>("Inventories retrieved", Arrays.asList(responseDto)));

        // When & Then
        mockMvc.perform(get("/api/inventories")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1));

        verify(inventoryService).getAllInventories();
    }

    @Test
    void getInventoriesByWarehouse_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(inventoryService.getInventoriesByWarehouse(1L))
                .thenReturn(new ApiResponse<>("Inventories retrieved", Arrays.asList(responseDto)));

        // When & Then
        mockMvc.perform(get("/api/inventories/warehouse/1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        verify(inventoryService).getInventoriesByWarehouse(1L);
    }

    @Test
    void getInventoriesByProduct_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(inventoryService.getInventoriesByProduct(1L))
                .thenReturn(new ApiResponse<>("Inventories retrieved", Arrays.asList(responseDto)));

        // When & Then
        mockMvc.perform(get("/api/inventories/product/1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        verify(inventoryService).getInventoriesByProduct(1L);
    }

    @Test
    void getLowStockInWarehouse_WithDefaultThreshold_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(inventoryService.getLowStockInWarehouse(1L, 10))
                .thenReturn(new ApiResponse<>("Low stock items", Arrays.asList(responseDto)));

        // When & Then
        mockMvc.perform(get("/api/inventories/warehouse/1/low-stock")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        verify(inventoryService).getLowStockInWarehouse(1L, 10);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // UPDATE INVENTORY TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void updateInventory_AsAdmin_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(inventoryService.updateInventory(eq(1L), any(InventoryRequestDto.class)))
                .thenReturn(new ApiResponse<>("Inventory updated", responseDto));

        // When & Then
        mockMvc.perform(put("/api/inventories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Inventory updated"));

        verify(inventoryService).updateInventory(eq(1L), any(InventoryRequestDto.class));
    }

    @Test
    void updateInventory_AsNonAdmin_ReturnsForbidden() throws Exception {
        // Given
        doThrow(new AccessDeniedException("Access denied"))
                .when(permissionService).requireAdmin(any());

        // When & Then
        mockMvc.perform(put("/api/inventories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isForbidden());

        verify(inventoryService, never()).updateInventory(anyLong(), any());
    }

    @Test
    void adjustQuantities_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(inventoryService.adjustQuantities(1L, 150, 20))
                .thenReturn(new ApiResponse<>("Quantities adjusted", responseDto));

        // When & Then
        mockMvc.perform(patch("/api/inventories/1/adjust")
                        .param("qtyOnHand", "150")
                        .param("qtyReserved", "20")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Quantities adjusted"));

        verify(inventoryService).adjustQuantities(1L, 150, 20);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // DELETE INVENTORY TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void deleteInventory_AsAdmin_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(inventoryService.deleteInventory(1L))
                .thenReturn(new ApiResponse<>("Inventory deleted", null));

        // When & Then
        mockMvc.perform(delete("/api/inventories/1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Inventory deleted"));

        verify(inventoryService).deleteInventory(1L);
    }

    @Test
    void deleteInventory_AsNonAdmin_ReturnsForbidden() throws Exception {
        // Given
        doThrow(new AccessDeniedException("Access denied"))
                .when(permissionService).requireAdmin(any());

        // When & Then
        mockMvc.perform(delete("/api/inventories/1")
                        .session(session))
                .andExpect(status().isForbidden());

        verify(inventoryService, never()).deleteInventory(anyLong());
    }

    @Test
    void deleteInventoryByWarehouseAndProduct_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(inventoryService.deleteInventoryByWarehouseAndProduct(1L, 1L))
                .thenReturn(new ApiResponse<>("Inventory deleted", null));

        // When & Then
        mockMvc.perform(delete("/api/inventories/warehouse/1/product/1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Inventory deleted"));

        verify(inventoryService).deleteInventoryByWarehouseAndProduct(1L, 1L);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // STOCK QUERIES & COUNT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getTotalStockByProduct_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(inventoryService.getTotalStockByProduct(1L))
                .thenReturn(new ApiResponse<>("Total stock retrieved", 500));

        // When & Then
        mockMvc.perform(get("/api/inventories/product/1/total-stock")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(500));

        verify(inventoryService).getTotalStockByProduct(1L);
    }

    @Test
    void getAvailableStockByProduct_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(inventoryService.getAvailableStockByProduct(1L))
                .thenReturn(new ApiResponse<>("Available stock retrieved", 450));

        // When & Then
        mockMvc.perform(get("/api/inventories/product/1/available-stock")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(450));

        verify(inventoryService).getAvailableStockByProduct(1L);
    }

    @Test
    void countInventories_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(inventoryService.countInventories())
                .thenReturn(new ApiResponse<>("Total inventories", 75L));

        // When & Then
        mockMvc.perform(get("/api/inventories/count")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(75));

        verify(inventoryService).countInventories();
    }
}