package org.project.digital_logistics.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.shipment.ShipmentResponseDto;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.model.enums.ShipmentStatus;
import org.project.digital_logistics.service.PermissionService;
import org.project.digital_logistics.service.ShipmentService;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ShipmentController.class)
class ShipmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShipmentService shipmentService;

    @MockBean
    private PermissionService permissionService;

    private MockHttpSession session;
    private ShipmentResponseDto responseDto;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();

        // Setup ShipmentResponseDto
        responseDto = new ShipmentResponseDto();
        responseDto.setId(1L);
        responseDto.setTrackingNumber("SHIP-12345");
        responseDto.setSalesOrderId(1L);
        responseDto.setCarrierId(1L);
        responseDto.setStatus(ShipmentStatus.PLANNED);  // ✅ Changed from PENDING
        responseDto.setPlannedDate(LocalDateTime.now().plusDays(3));
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // GET SHIPMENT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getShipmentById_Success_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(shipmentService.getShipmentById(1L))
                .thenReturn(new ApiResponse<>("Shipment retrieved successfully", responseDto));

        // When & Then
        mockMvc.perform(get("/api/shipments/1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Shipment retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.trackingNumber").value("SHIP-12345"));

        verify(permissionService).requireWarehouseManager(any());
        verify(shipmentService).getShipmentById(1L);
    }

    @Test
    void getShipmentById_NotFound_ReturnsNotFound() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(shipmentService.getShipmentById(999L))
                .thenThrow(new ResourceNotFoundException("Shipment", "id", 999L));

        // When & Then
        mockMvc.perform(get("/api/shipments/999")
                        .session(session))
                .andExpect(status().isNotFound());

        verify(shipmentService).getShipmentById(999L);
    }

    @Test
    void getShipmentBySalesOrder_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(shipmentService.getShipmentBySalesOrder(1L))
                .thenReturn(new ApiResponse<>("Shipment retrieved", responseDto));

        // When & Then
        mockMvc.perform(get("/api/shipments/sales-order/1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.salesOrderId").value(1));

        verify(shipmentService).getShipmentBySalesOrder(1L);
    }

    @Test
    void trackShipment_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(shipmentService.getShipmentByTrackingNumber("SHIP-12345"))
                .thenReturn(new ApiResponse<>("Shipment tracked", responseDto));

        // When & Then
        mockMvc.perform(get("/api/shipments/track/SHIP-12345")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trackingNumber").value("SHIP-12345"));

        verify(shipmentService).getShipmentByTrackingNumber("SHIP-12345");
    }

    @Test
    void getAllShipments_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(shipmentService.getAllShipments())
                .thenReturn(new ApiResponse<>("Shipments retrieved", Arrays.asList(responseDto)));

        // When & Then
        mockMvc.perform(get("/api/shipments")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].trackingNumber").value("SHIP-12345"));

        verify(shipmentService).getAllShipments();
    }

    @Test
    void getShipmentsByStatus_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(shipmentService.getShipmentsByStatus(ShipmentStatus.PLANNED))  // ✅ Changed from PENDING
                .thenReturn(new ApiResponse<>("Shipments retrieved", Arrays.asList(responseDto)));

        // When & Then
        mockMvc.perform(get("/api/shipments/status/PLANNED")  // ✅ Changed from PENDING
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        verify(shipmentService).getShipmentsByStatus(ShipmentStatus.PLANNED);  // ✅ Changed from PENDING
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // UPDATE SHIPMENT STATUS TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void markAsInTransit_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(shipmentService.markAsInTransit(1L))
                .thenReturn(new ApiResponse<>("Shipment marked as in transit", responseDto));

        // When & Then
        mockMvc.perform(patch("/api/shipments/1/in-transit")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Shipment marked as in transit"));

        verify(shipmentService).markAsInTransit(1L);
    }

    @Test
    void markAsDelivered_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(shipmentService.markAsDelivered(1L))
                .thenReturn(new ApiResponse<>("Shipment marked as delivered", responseDto));

        // When & Then
        mockMvc.perform(patch("/api/shipments/1/deliver")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Shipment marked as delivered"));

        verify(shipmentService).markAsDelivered(1L);
    }

    @Test
    void updatePlannedDate_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        LocalDateTime newDate = LocalDateTime.now().plusDays(5);
        when(shipmentService.updatePlannedDate(eq(1L), any(LocalDateTime.class)))
                .thenReturn(new ApiResponse<>("Planned date updated", responseDto));

        // When & Then
        mockMvc.perform(patch("/api/shipments/1/planned-date")
                        .param("plannedDate", newDate.toString())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Planned date updated"));

        verify(shipmentService).updatePlannedDate(eq(1L), any(LocalDateTime.class));
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // DELETE & COUNT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void deleteShipment_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(shipmentService.deleteShipment(1L))
                .thenReturn(new ApiResponse<>("Shipment deleted", null));

        // When & Then
        mockMvc.perform(delete("/api/shipments/1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Shipment deleted"));

        verify(shipmentService).deleteShipment(1L);
    }

    @Test
    void countShipments_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(shipmentService.countShipments())
                .thenReturn(new ApiResponse<>("Total shipments", 100L));

        // When & Then
        mockMvc.perform(get("/api/shipments/count")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(100));

        verify(shipmentService).countShipments();
    }

    @Test
    void countShipmentsByStatus_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(shipmentService.countShipmentsByStatus(ShipmentStatus.PLANNED))  // ✅ Changed from PENDING
                .thenReturn(new ApiResponse<>("Shipments count", 25L));

        // When & Then
        mockMvc.perform(get("/api/shipments/count/status/PLANNED")  // ✅ Changed from PENDING
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(25));

        verify(shipmentService).countShipmentsByStatus(ShipmentStatus.PLANNED);  // ✅ Changed from PENDING
    }
}