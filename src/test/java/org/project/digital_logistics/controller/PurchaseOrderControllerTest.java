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
import org.project.digital_logistics.dto.purchaseorder.PurchaseOrderLineDto;
import org.project.digital_logistics.dto.purchaseorder.PurchaseOrderRequestDto;
import org.project.digital_logistics.dto.purchaseorder.PurchaseOrderResponseDto;
import org.project.digital_logistics.enums.PurchaseOrderStatus;
import org.project.digital_logistics.exception.AccessDeniedException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.service.PermissionService;
import org.project.digital_logistics.service.PurchaseOrderService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PurchaseOrderController.class)
class PurchaseOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PurchaseOrderService purchaseOrderService;

    @MockBean
    private PermissionService permissionService;

    private MockHttpSession session;
    private PurchaseOrderRequestDto requestDto;
    private PurchaseOrderResponseDto responseDto;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();

        // Setup PurchaseOrderLineDto (nested DTO)
        PurchaseOrderLineDto orderLine = PurchaseOrderLineDto.builder()
                .productId(1L)
                .quantity(100)
                .unitPrice(BigDecimal.valueOf(50.00))
                .build();

        // Setup PurchaseOrderRequestDto with orderLines
        requestDto = PurchaseOrderRequestDto.builder()
                .supplierId(1L)
                .expectedDelivery(LocalDateTime.now().plusDays(7))
                .orderLines(Arrays.asList(orderLine))
                .build();

        // Setup PurchaseOrderResponseDto
        responseDto = new PurchaseOrderResponseDto();
        responseDto.setId(1L);
        responseDto.setSupplierId(1L);
        responseDto.setStatus(PurchaseOrderStatus.CREATED);
        responseDto.setTotalAmount(BigDecimal.valueOf(5000.00));
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CREATE PURCHASE ORDER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void createPurchaseOrder_AsWarehouseManager_ReturnsCreated() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(purchaseOrderService.createPurchaseOrder(any(PurchaseOrderRequestDto.class)))
                .thenReturn(new ApiResponse<>("Purchase order created", responseDto));

        // When & Then
        mockMvc.perform(post("/api/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Purchase order created"))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(permissionService).requireWarehouseManager(any());
        verify(purchaseOrderService).createPurchaseOrder(any(PurchaseOrderRequestDto.class));
    }

    @Test
    void createPurchaseOrder_AsClient_ReturnsForbidden() throws Exception {
        // Given
        doThrow(new AccessDeniedException("Access denied"))
                .when(permissionService).requireWarehouseManager(any());

        // When & Then
        mockMvc.perform(post("/api/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isForbidden());

        verify(purchaseOrderService, never()).createPurchaseOrder(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // GET PURCHASE ORDER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getPurchaseOrderById_Success_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(purchaseOrderService.getPurchaseOrderById(1L))
                .thenReturn(new ApiResponse<>("Purchase order retrieved", responseDto));

        // When & Then
        mockMvc.perform(get("/api/purchase-orders/1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("CREATED"));

        verify(purchaseOrderService).getPurchaseOrderById(1L);
    }

    @Test
    void getPurchaseOrderById_NotFound_ReturnsNotFound() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(purchaseOrderService.getPurchaseOrderById(999L))
                .thenThrow(new ResourceNotFoundException("PurchaseOrder", "id", 999L));

        // When & Then
        mockMvc.perform(get("/api/purchase-orders/999")
                        .session(session))
                .andExpect(status().isNotFound());

        verify(purchaseOrderService).getPurchaseOrderById(999L);
    }

    @Test
    void getAllPurchaseOrders_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(purchaseOrderService.getAllPurchaseOrders())
                .thenReturn(new ApiResponse<>("Purchase orders retrieved", Arrays.asList(responseDto)));

        // When & Then
        mockMvc.perform(get("/api/purchase-orders")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1));

        verify(purchaseOrderService).getAllPurchaseOrders();
    }

    @Test
    void getPurchaseOrdersByStatus_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(purchaseOrderService.getPurchaseOrdersByStatus(PurchaseOrderStatus.CREATED))
                .thenReturn(new ApiResponse<>("Orders retrieved", Arrays.asList(responseDto)));

        // When & Then
        mockMvc.perform(get("/api/purchase-orders/status/CREATED")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        verify(purchaseOrderService).getPurchaseOrdersByStatus(PurchaseOrderStatus.CREATED);
    }

    @Test
    void getPurchaseOrdersBySupplier_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(purchaseOrderService.getPurchaseOrdersBySupplier(1L))
                .thenReturn(new ApiResponse<>("Orders retrieved", Arrays.asList(responseDto)));

        // When & Then
        mockMvc.perform(get("/api/purchase-orders/supplier/1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        verify(purchaseOrderService).getPurchaseOrdersBySupplier(1L);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // UPDATE PURCHASE ORDER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void updatePurchaseOrder_AsWarehouseManager_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(purchaseOrderService.updatePurchaseOrder(eq(1L), any(PurchaseOrderRequestDto.class)))
                .thenReturn(new ApiResponse<>("Order updated", responseDto));

        // When & Then
        mockMvc.perform(put("/api/purchase-orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Order updated"));

        verify(purchaseOrderService).updatePurchaseOrder(eq(1L), any(PurchaseOrderRequestDto.class));
    }

    @Test
    void updatePurchaseOrder_AsClient_ReturnsForbidden() throws Exception {
        // Given
        doThrow(new AccessDeniedException("Access denied"))
                .when(permissionService).requireWarehouseManager(any());

        // When & Then
        mockMvc.perform(put("/api/purchase-orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isForbidden());

        verify(purchaseOrderService, never()).updatePurchaseOrder(anyLong(), any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // APPROVE PURCHASE ORDER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void approvePurchaseOrder_AsAdmin_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(purchaseOrderService.approvePurchaseOrder(1L))
                .thenReturn(new ApiResponse<>("Order approved", responseDto));

        // When & Then
        mockMvc.perform(patch("/api/purchase-orders/1/approve")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Order approved"));

        verify(purchaseOrderService).approvePurchaseOrder(1L);
    }

    @Test
    void approvePurchaseOrder_AsNonAdmin_ReturnsForbidden() throws Exception {
        // Given
        doThrow(new AccessDeniedException("Access denied"))
                .when(permissionService).requireAdmin(any());

        // When & Then
        mockMvc.perform(patch("/api/purchase-orders/1/approve")
                        .session(session))
                .andExpect(status().isForbidden());

        verify(purchaseOrderService, never()).approvePurchaseOrder(anyLong());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // RECEIVE PURCHASE ORDER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void receivePurchaseOrder_AsAdmin_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(purchaseOrderService.receivePurchaseOrder(1L, 1L))
                .thenReturn(new ApiResponse<>("Order received", responseDto));

        // When & Then
        mockMvc.perform(patch("/api/purchase-orders/1/receive")
                        .param("warehouseId", "1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Order received"));

        verify(purchaseOrderService).receivePurchaseOrder(1L, 1L);
    }

    @Test
    void receivePurchaseOrder_AsNonAdmin_ReturnsForbidden() throws Exception {
        // Given
        doThrow(new AccessDeniedException("Access denied"))
                .when(permissionService).requireAdmin(any());

        // When & Then
        mockMvc.perform(patch("/api/purchase-orders/1/receive")
                        .param("warehouseId", "1")
                        .session(session))
                .andExpect(status().isForbidden());

        verify(purchaseOrderService, never()).receivePurchaseOrder(anyLong(), anyLong());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CANCEL PURCHASE ORDER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void cancelPurchaseOrder_AsAdmin_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(purchaseOrderService.cancelPurchaseOrder(1L))
                .thenReturn(new ApiResponse<>("Order cancelled", responseDto));

        // When & Then
        mockMvc.perform(patch("/api/purchase-orders/1/cancel")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Order cancelled"));

        verify(purchaseOrderService).cancelPurchaseOrder(1L);
    }

    @Test
    void cancelPurchaseOrder_AsNonAdmin_ReturnsForbidden() throws Exception {
        // Given
        doThrow(new AccessDeniedException("Access denied"))
                .when(permissionService).requireAdmin(any());

        // When & Then
        mockMvc.perform(patch("/api/purchase-orders/1/cancel")
                        .session(session))
                .andExpect(status().isForbidden());

        verify(purchaseOrderService, never()).cancelPurchaseOrder(anyLong());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // DELETE PURCHASE ORDER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void deletePurchaseOrder_AsAdmin_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(purchaseOrderService.deletePurchaseOrder(1L))
                .thenReturn(new ApiResponse<>("Order deleted", null));

        // When & Then
        mockMvc.perform(delete("/api/purchase-orders/1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Order deleted"));

        verify(purchaseOrderService).deletePurchaseOrder(1L);
    }

    @Test
    void deletePurchaseOrder_AsNonAdmin_ReturnsForbidden() throws Exception {
        // Given
        doThrow(new AccessDeniedException("Access denied"))
                .when(permissionService).requireAdmin(any());

        // When & Then
        mockMvc.perform(delete("/api/purchase-orders/1")
                        .session(session))
                .andExpect(status().isForbidden());

        verify(purchaseOrderService, never()).deletePurchaseOrder(anyLong());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // COUNT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void countPurchaseOrders_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(purchaseOrderService.countPurchaseOrders())
                .thenReturn(new ApiResponse<>("Total orders", 25L));

        // When & Then
        mockMvc.perform(get("/api/purchase-orders/count")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(25));

        verify(purchaseOrderService).countPurchaseOrders();
    }

    @Test
    void countPurchaseOrdersByStatus_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(purchaseOrderService.countPurchaseOrdersByStatus(PurchaseOrderStatus.CREATED))
                .thenReturn(new ApiResponse<>("Orders count", 10L));

        // When & Then
        mockMvc.perform(get("/api/purchase-orders/count/status/CREATED")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(10));

        verify(purchaseOrderService).countPurchaseOrdersByStatus(PurchaseOrderStatus.CREATED);
    }
}