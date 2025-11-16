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
import org.project.digital_logistics.dto.salesorder.SalesOrderLineDto;
import org.project.digital_logistics.dto.salesorder.SalesOrderRequestDto;
import org.project.digital_logistics.dto.salesorder.SalesOrderResponseDto;
import org.project.digital_logistics.exception.AccessDeniedException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.model.User;
import org.project.digital_logistics.model.enums.OrderStatus;
import org.project.digital_logistics.model.enums.Role;
import org.project.digital_logistics.service.PermissionService;
import org.project.digital_logistics.service.SalesOrderService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SalesOrderController.class)
class SalesOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SalesOrderService salesOrderService;

    @MockBean
    private PermissionService permissionService;

    private MockHttpSession session;
    private SalesOrderRequestDto requestDto;
    private SalesOrderResponseDto responseDto;
    private User clientUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();

        // Setup Client User
        clientUser = User.builder()
                .id(1L)
                .name("client_user")
                .email("client@example.com")
                .role(Role.CLIENT)
                .active(true)
                .build();

        // Setup Admin User
        adminUser = User.builder()
                .id(2L)
                .name("admin_user")
                .email("admin@example.com")
                .role(Role.ADMIN)
                .active(true)
                .build();

        // Setup SalesOrderLineDto
        SalesOrderLineDto orderLine = SalesOrderLineDto.builder()
                .productId(1L)
                .quantity(10)
                .unitPrice(BigDecimal.valueOf(100.00))
                .build();

        // Setup SalesOrderRequestDto
        requestDto = SalesOrderRequestDto.builder()
                .orderLines(Arrays.asList(orderLine))
                .build();

        // Setup SalesOrderResponseDto
        responseDto = new SalesOrderResponseDto();
        responseDto.setId(1L);
        responseDto.setClientId(1L);
        responseDto.setStatus(OrderStatus.CREATED);
        responseDto.setTotalAmount(BigDecimal.valueOf(1000.00));
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CREATE SALES ORDER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void createSalesOrder_AsClient_ReturnsCreated() throws Exception {
        // Given
        when(permissionService.getAuthenticatedUser(any())).thenReturn(clientUser);
        when(salesOrderService.createSalesOrder(any(SalesOrderRequestDto.class), eq(1L)))
                .thenReturn(new ApiResponse<>("Sales order created", responseDto));

        // When & Then
        mockMvc.perform(post("/api/sales-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Sales order created"))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(permissionService).getAuthenticatedUser(any());
        verify(salesOrderService).createSalesOrder(any(SalesOrderRequestDto.class), eq(1L));
    }

    @Test
    void createSalesOrder_AsNonClient_ReturnsForbidden() throws Exception {
        // Given
        when(permissionService.getAuthenticatedUser(any())).thenReturn(adminUser);

        // When & Then
        mockMvc.perform(post("/api/sales-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isForbidden());

        verify(salesOrderService, never()).createSalesOrder(any(), anyLong());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // GET SALES ORDER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getSalesOrderById_Success_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(salesOrderService.getSalesOrderById(1L))
                .thenReturn(new ApiResponse<>("Sales order retrieved", responseDto));

        // When & Then
        mockMvc.perform(get("/api/sales-orders/1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("CREATED"));

        verify(salesOrderService).getSalesOrderById(1L);
    }

    @Test
    void getSalesOrderById_NotFound_ReturnsNotFound() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(salesOrderService.getSalesOrderById(999L))
                .thenThrow(new ResourceNotFoundException("SalesOrder", "id", 999L));

        // When & Then
        mockMvc.perform(get("/api/sales-orders/999")
                        .session(session))
                .andExpect(status().isNotFound());

        verify(salesOrderService).getSalesOrderById(999L);
    }

    @Test
    void getAllSalesOrders_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(salesOrderService.getAllSalesOrders())
                .thenReturn(new ApiResponse<>("Sales orders retrieved", Arrays.asList(responseDto)));

        // When & Then
        mockMvc.perform(get("/api/sales-orders")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1));

        verify(salesOrderService).getAllSalesOrders();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // MY ORDERS TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getMyOrders_AsClient_ReturnsOk() throws Exception {
        // Given
        when(permissionService.getAuthenticatedUser(any())).thenReturn(clientUser);
        when(salesOrderService.getSalesOrdersByClient(1L))
                .thenReturn(new ApiResponse<>("My orders retrieved", Arrays.asList(responseDto)));

        // When & Then
        mockMvc.perform(get("/api/sales-orders/my-orders")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        verify(salesOrderService).getSalesOrdersByClient(1L);
    }

    @Test
    void getMyOrders_AsNonClient_ReturnsForbidden() throws Exception {
        // Given
        when(permissionService.getAuthenticatedUser(any())).thenReturn(adminUser);

        // When & Then
        mockMvc.perform(get("/api/sales-orders/my-orders")
                        .session(session))
                .andExpect(status().isForbidden());

        verify(salesOrderService, never()).getSalesOrdersByClient(anyLong());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // FILTER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getSalesOrdersByStatus_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(salesOrderService.getSalesOrdersByStatus(OrderStatus.CREATED))
                .thenReturn(new ApiResponse<>("Orders retrieved", Arrays.asList(responseDto)));

        // When & Then
        mockMvc.perform(get("/api/sales-orders/status/CREATED")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        verify(salesOrderService).getSalesOrdersByStatus(OrderStatus.CREATED);
    }

    @Test
    void getSalesOrdersByClient_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(salesOrderService.getSalesOrdersByClient(1L))
                .thenReturn(new ApiResponse<>("Orders retrieved", Arrays.asList(responseDto)));

        // When & Then
        mockMvc.perform(get("/api/sales-orders/client/1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        verify(salesOrderService).getSalesOrdersByClient(1L);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // ORDER WORKFLOW TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void reserveStock_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(salesOrderService.reserveStock(1L))
                .thenReturn(new ApiResponse<>("Stock reserved", responseDto));

        // When & Then
        mockMvc.perform(patch("/api/sales-orders/1/reserve")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Stock reserved"));

        verify(salesOrderService).reserveStock(1L);
    }

    @Test
    void shipOrder_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(salesOrderService.shipOrder(1L))
                .thenReturn(new ApiResponse<>("Order shipped", responseDto));

        // When & Then
        mockMvc.perform(patch("/api/sales-orders/1/ship")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Order shipped"));

        verify(salesOrderService).shipOrder(1L);
    }

    @Test
    void deliverOrder_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(salesOrderService.deliverOrder(1L))
                .thenReturn(new ApiResponse<>("Order delivered", responseDto));

        // When & Then
        mockMvc.perform(patch("/api/sales-orders/1/deliver")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Order delivered"));

        verify(salesOrderService).deliverOrder(1L);
    }

    @Test
    void cancelOrder_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(salesOrderService.cancelOrder(1L))
                .thenReturn(new ApiResponse<>("Order cancelled", responseDto));

        // When & Then
        mockMvc.perform(patch("/api/sales-orders/1/cancel")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Order cancelled"));

        verify(salesOrderService).cancelOrder(1L);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // COUNT TEST
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void countSalesOrders_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(salesOrderService.countSalesOrders())
                .thenReturn(new ApiResponse<>("Total orders", 50L));

        // When & Then
        mockMvc.perform(get("/api/sales-orders/count")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(50));

        verify(salesOrderService).countSalesOrders();
    }
}