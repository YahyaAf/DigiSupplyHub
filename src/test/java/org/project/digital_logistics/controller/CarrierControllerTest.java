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
import org.project.digital_logistics.dto.carrier.CarrierRequestDto;
import org.project.digital_logistics.dto.carrier.CarrierResponseDto;
import org.project.digital_logistics.dto.shipment.ShipmentResponseDto;
import org.project.digital_logistics.exception.AccessDeniedException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.model.enums.CarrierStatus;
import org.project.digital_logistics.service.CarrierService;
import org.project.digital_logistics.service.PermissionService;
import org.project.digital_logistics.service.ShipmentService;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CarrierController.class)
class CarrierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CarrierService carrierService;

    @MockBean
    private ShipmentService shipmentService;

    @MockBean
    private PermissionService permissionService;

    private MockHttpSession session;
    private CarrierRequestDto requestDto;
    private CarrierResponseDto responseDto;
    private ShipmentResponseDto shipmentResponseDto;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();

        // Setup CarrierRequestDto
        requestDto = new CarrierRequestDto();
        requestDto.setCode("DHL-001");
        requestDto.setName("DHL Express");
        requestDto.setContactEmail("contact@dhl.com");
        requestDto.setContactPhone("0612345678");
        requestDto.setBaseShippingRate(BigDecimal.valueOf(50));
        requestDto.setMaxDailyCapacity(100);
        requestDto.setCutOffTime(LocalTime.of(17, 0));

        // Setup CarrierResponseDto
        responseDto = new CarrierResponseDto();
        responseDto.setId(1L);
        responseDto.setCode("DHL-001");
        responseDto.setName("DHL Express");
        responseDto.setStatus(CarrierStatus.ACTIVE);

        // Setup ShipmentResponseDto
        shipmentResponseDto = new ShipmentResponseDto();
        shipmentResponseDto.setId(1L);
        shipmentResponseDto.setTrackingNumber("TRACK-123");
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CREATE CARRIER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void createCarrier_AsAdmin_ReturnsCreated() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(carrierService.createCarrier(any(CarrierRequestDto.class)))
                .thenReturn(new ApiResponse<>("Carrier created successfully", responseDto));

        // When & Then
        mockMvc.perform(post("/api/carriers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Carrier created successfully"))
                .andExpect(jsonPath("$.data.code").value("DHL-001"));

        verify(permissionService).requireAdmin(any());
        verify(carrierService).createCarrier(any(CarrierRequestDto.class));
    }

    @Test
    void createCarrier_AsNonAdmin_ReturnsForbidden() throws Exception {
        // Given
        doThrow(new AccessDeniedException("Access denied. Admin privileges required."))
                .when(permissionService).requireAdmin(any());

        // When & Then
        mockMvc.perform(post("/api/carriers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isForbidden());

        verify(permissionService).requireAdmin(any());
        verify(carrierService, never()).createCarrier(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // GET CARRIER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getCarrierById_Success_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(carrierService.getCarrierById(1L))
                .thenReturn(new ApiResponse<>("Carrier retrieved successfully", responseDto));

        // When & Then
        mockMvc.perform(get("/api/carriers/1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.code").value("DHL-001"));

        verify(permissionService).requireWarehouseManager(any());
        verify(carrierService).getCarrierById(1L);
    }

    @Test
    void getCarrierById_NotFound_ReturnsNotFound() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(carrierService.getCarrierById(999L))
                .thenThrow(new ResourceNotFoundException("Carrier", "id", 999L));

        // When & Then
        mockMvc.perform(get("/api/carriers/999")
                        .session(session))
                .andExpect(status().isNotFound());

        verify(carrierService).getCarrierById(999L);
    }

    @Test
    void getCarrierByCode_Success_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(carrierService.getCarrierByCode("DHL-001"))
                .thenReturn(new ApiResponse<>("Carrier retrieved successfully", responseDto));

        // When & Then
        mockMvc.perform(get("/api/carriers/code/DHL-001")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("DHL-001"));

        verify(carrierService).getCarrierByCode("DHL-001");
    }

    @Test
    void getAllCarriers_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(carrierService.getAllCarriers())
                .thenReturn(new ApiResponse<>("Carriers retrieved", Arrays.asList(responseDto)));

        // When & Then
        mockMvc.perform(get("/api/carriers")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].code").value("DHL-001"));

        verify(carrierService).getAllCarriers();
    }

    @Test
    void getCarriersByStatus_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(carrierService.getCarriersByStatus(CarrierStatus.ACTIVE))
                .thenReturn(new ApiResponse<>("Carriers retrieved", Arrays.asList(responseDto)));

        // When & Then
        mockMvc.perform(get("/api/carriers/status/ACTIVE")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        verify(carrierService).getCarriersByStatus(CarrierStatus.ACTIVE);
    }

    @Test
    void getAvailableCarriers_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(carrierService.getAvailableCarriers())
                .thenReturn(new ApiResponse<>("Available carriers retrieved", Arrays.asList(responseDto)));

        // When & Then
        mockMvc.perform(get("/api/carriers/available")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        verify(carrierService).getAvailableCarriers();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // UPDATE CARRIER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void updateCarrier_AsAdmin_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(carrierService.updateCarrier(eq(1L), any(CarrierRequestDto.class)))
                .thenReturn(new ApiResponse<>("Carrier updated successfully", responseDto));

        // When & Then
        mockMvc.perform(put("/api/carriers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Carrier updated successfully"));

        verify(permissionService).requireAdmin(any());
        verify(carrierService).updateCarrier(eq(1L), any(CarrierRequestDto.class));
    }

    @Test
    void updateCarrier_AsNonAdmin_ReturnsForbidden() throws Exception {
        // Given
        doThrow(new AccessDeniedException("Access denied"))
                .when(permissionService).requireAdmin(any());

        // When & Then
        mockMvc.perform(put("/api/carriers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isForbidden());

        verify(carrierService, never()).updateCarrier(anyLong(), any());
    }

    @Test
    void updateCarrierStatus_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(carrierService.updateCarrierStatus(1L, CarrierStatus.SUSPENDED))
                .thenReturn(new ApiResponse<>("Status updated", responseDto));

        // When & Then
        mockMvc.perform(patch("/api/carriers/1/status")
                        .param("status", "SUSPENDED")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Status updated"));

        verify(carrierService).updateCarrierStatus(1L, CarrierStatus.SUSPENDED);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // SHIPMENT ASSIGNMENT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void assignShipment_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(shipmentService.assignCarrier(1L, 1L))
                .thenReturn(new ApiResponse<>("Shipment assigned", shipmentResponseDto));

        // When & Then
        mockMvc.perform(patch("/api/carriers/1/assign-shipment/1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trackingNumber").value("TRACK-123"));

        verify(shipmentService).assignCarrier(1L, 1L);
    }

    @Test
    void assignMultipleShipments_ReturnsOk() throws Exception {
        // Given
        List<Long> shipmentIds = Arrays.asList(1L, 2L);
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(shipmentService.assignMultipleShipments(1L, shipmentIds))
                .thenReturn(new ApiResponse<>("Shipments assigned", Arrays.asList(shipmentResponseDto)));

        // When & Then
        mockMvc.perform(patch("/api/carriers/1/assign-multiple")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shipmentIds))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        verify(shipmentService).assignMultipleShipments(1L, shipmentIds);
    }

    @Test
    void getCarrierShipments_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(shipmentService.getShipmentsByCarrier(1L))
                .thenReturn(new ApiResponse<>("Shipments retrieved", Arrays.asList(shipmentResponseDto)));

        // When & Then
        mockMvc.perform(get("/api/carriers/1/shipments")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        verify(shipmentService).getShipmentsByCarrier(1L);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // DELETE & UTILITY TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void deleteCarrier_AsAdmin_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(carrierService.deleteCarrier(1L))
                .thenReturn(new ApiResponse<>("Carrier deleted successfully", null));

        // When & Then
        mockMvc.perform(delete("/api/carriers/1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Carrier deleted successfully"));

        verify(permissionService).requireAdmin(any());
        verify(carrierService).deleteCarrier(1L);
    }

    @Test
    void deleteCarrier_AsNonAdmin_ReturnsForbidden() throws Exception {
        // Given
        doThrow(new AccessDeniedException("Access denied"))
                .when(permissionService).requireAdmin(any());

        // When & Then
        mockMvc.perform(delete("/api/carriers/1")
                        .session(session))
                .andExpect(status().isForbidden());

        verify(carrierService, never()).deleteCarrier(anyLong());
    }

    @Test
    void countCarriers_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireWarehouseManager(any());
        when(carrierService.countCarriers())
                .thenReturn(new ApiResponse<>("Total carriers counted", 10L));

        // When & Then
        mockMvc.perform(get("/api/carriers/count")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(10));

        verify(carrierService).countCarriers();
    }

    @Test
    void resetDailyShipments_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(carrierService.resetDailyShipments())
                .thenReturn(new ApiResponse<>("Daily shipments reset", null));

        // When & Then
        mockMvc.perform(post("/api/carriers/reset-daily-shipments")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Daily shipments reset"));

        verify(permissionService).requireAdmin(any());
        verify(carrierService).resetDailyShipments();
    }
}