package org.project.digital_logistics.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.shipment.ShipmentResponseDto;
import org.project.digital_logistics.exception.InvalidOperationException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.model.*;
import org.project.digital_logistics.model.enums.CarrierStatus;
import org.project.digital_logistics.model.enums.OrderStatus;
import org.project.digital_logistics.model.enums.ShipmentStatus;
import org.project.digital_logistics.repository.CarrierRepository;
import org.project.digital_logistics.repository.SalesOrderRepository;
import org.project.digital_logistics.repository.ShipmentRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private SalesOrderRepository salesOrderRepository;

    @Mock
    private CarrierRepository carrierRepository;

    @Mock
    private CarrierService carrierService;

    @InjectMocks
    private ShipmentService shipmentService;

    private Shipment shipment;
    private SalesOrder salesOrder;
    private Carrier carrier;
    private Client client;

    @BeforeEach
    void setUp() {
        // Setup Client
        client = Client.builder()
                .id(1L)
                .name("Test Client")
                .email("client@test.com")
                .phoneNumber("0612345678")
                .address("123 Test St")
                .active(true)
                .build();

        // Setup Carrier
        carrier = Carrier.builder()
                .id(1L)
                .code("DHL-001")
                .name("DHL Express")
                .contactEmail("contact@dhl.com")
                .contactPhone("0612345678")
                .baseShippingRate(BigDecimal.valueOf(50))
                .maxDailyCapacity(100)
                .currentDailyShipments(10)
                .cutOffTime(LocalTime.of(17, 0))
                .status(CarrierStatus.ACTIVE)
                .build();

        // Setup SalesOrder
        salesOrder = SalesOrder.builder()
                .id(1L)
                .client(client)
                .status(OrderStatus.SHIPPED)
                .build();

        // Setup Shipment
        shipment = Shipment.builder()
                .id(1L)
                .trackingNumber("TRACK-12345")
                .salesOrder(salesOrder)
                .carrier(carrier)
                .status(ShipmentStatus.PLANNED)
                .plannedDate(LocalDateTime.now().plusDays(1))
                .build();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // AUTO CREATE SHIPMENT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void autoCreateShipment_NewShipment_Success() {
        // Given
        when(shipmentRepository.existsBySalesOrderId(1L)).thenReturn(false);
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(shipment);

        // When
        Shipment result = shipmentService.autoCreateShipment(salesOrder);

        // Then
        assertNotNull(result);
        verify(shipmentRepository).existsBySalesOrderId(1L);
        verify(shipmentRepository).save(any(Shipment.class));
    }

    @Test
    void autoCreateShipment_AlreadyExists_ReturnsExisting() {
        // Given
        when(shipmentRepository.existsBySalesOrderId(1L)).thenReturn(true);
        when(shipmentRepository.findBySalesOrderId(1L)).thenReturn(Optional.of(shipment));

        // When
        Shipment result = shipmentService.autoCreateShipment(salesOrder);

        // Then
        assertNotNull(result);
        assertEquals(shipment.getId(), result.getId());
        verify(shipmentRepository).existsBySalesOrderId(1L);
        verify(shipmentRepository, never()).save(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // GET SHIPMENT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getShipmentById_Success() {
        // Given
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));

        // When
        ApiResponse<ShipmentResponseDto> response = shipmentService.getShipmentById(1L);

        // Then
        assertNotNull(response);
        assertEquals("Shipment retrieved successfully", response.getMessage());
        assertEquals(1L, response.getData().getId());
        verify(shipmentRepository).findById(1L);
    }

    @Test
    void getShipmentById_NotFound_ThrowsException() {
        // Given
        when(shipmentRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> shipmentService.getShipmentById(999L));

        verify(shipmentRepository).findById(999L);
    }

    @Test
    void getShipmentBySalesOrder_Success() {
        // Given
        when(shipmentRepository.findBySalesOrderId(1L)).thenReturn(Optional.of(shipment));

        // When
        ApiResponse<ShipmentResponseDto> response =
                shipmentService.getShipmentBySalesOrder(1L);

        // Then
        assertNotNull(response);
        assertEquals("Shipment retrieved successfully", response.getMessage());
        verify(shipmentRepository).findBySalesOrderId(1L);
    }

    @Test
    void getShipmentBySalesOrder_NotFound_ThrowsException() {
        // Given
        when(shipmentRepository.findBySalesOrderId(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> shipmentService.getShipmentBySalesOrder(999L));

        verify(shipmentRepository).findBySalesOrderId(999L);
    }

    @Test
    void getShipmentByTrackingNumber_Success() {
        // Given
        when(shipmentRepository.findByTrackingNumber("TRACK-12345"))
                .thenReturn(Optional.of(shipment));

        // When
        ApiResponse<ShipmentResponseDto> response =
                shipmentService.getShipmentByTrackingNumber("TRACK-12345");

        // Then
        assertNotNull(response);
        assertEquals("Shipment retrieved successfully", response.getMessage());
        verify(shipmentRepository).findByTrackingNumber("TRACK-12345");
    }

    @Test
    void getShipmentByTrackingNumber_NotFound_ThrowsException() {
        // Given
        when(shipmentRepository.findByTrackingNumber("INVALID"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> shipmentService.getShipmentByTrackingNumber("INVALID"));

        verify(shipmentRepository).findByTrackingNumber("INVALID");
    }

    @Test
    void getAllShipments_Success() {
        // Given
        when(shipmentRepository.findAll()).thenReturn(Arrays.asList(shipment));

        // When
        ApiResponse<List<ShipmentResponseDto>> response = shipmentService.getAllShipments();

        // Then
        assertNotNull(response);
        assertEquals(1, response.getData().size());
        verify(shipmentRepository).findAll();
    }

    @Test
    void getShipmentsByStatus_Success() {
        // Given
        when(shipmentRepository.findByStatus(ShipmentStatus.PLANNED))
                .thenReturn(Arrays.asList(shipment));

        // When
        ApiResponse<List<ShipmentResponseDto>> response =
                shipmentService.getShipmentsByStatus(ShipmentStatus.PLANNED);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getData().size());
        verify(shipmentRepository).findByStatus(ShipmentStatus.PLANNED);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // STATUS UPDATE TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void markAsInTransit_Success() {
        // Given
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(shipment);

        // When
        ApiResponse<ShipmentResponseDto> response = shipmentService.markAsInTransit(1L);

        // Then
        assertNotNull(response);
        assertEquals("Shipment marked as IN_TRANSIT", response.getMessage());

        ArgumentCaptor<Shipment> captor = ArgumentCaptor.forClass(Shipment.class);
        verify(shipmentRepository).save(captor.capture());

        assertEquals(ShipmentStatus.IN_TRANSIT, captor.getValue().getStatus());
        assertNotNull(captor.getValue().getShippedDate());
    }

    @Test
    void markAsInTransit_NotPlanned_ThrowsException() {
        // Given
        shipment.setStatus(ShipmentStatus.IN_TRANSIT);
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));

        // When & Then
        assertThrows(InvalidOperationException.class,
                () -> shipmentService.markAsInTransit(1L));

        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void markAsDelivered_Success() {
        // Given
        shipment.setStatus(ShipmentStatus.IN_TRANSIT);
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(shipment);
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(salesOrder);
        doNothing().when(carrierService).decrementDailyShipments(1L);

        // When
        ApiResponse<ShipmentResponseDto> response = shipmentService.markAsDelivered(1L);

        // Then
        assertNotNull(response);
        assertTrue(response.getMessage().contains("DELIVERED"));

        verify(shipmentRepository).save(any(Shipment.class));
        verify(salesOrderRepository).save(any(SalesOrder.class));
        verify(carrierService).decrementDailyShipments(1L);
    }

    @Test
    void markAsDelivered_NotInTransit_ThrowsException() {
        // Given
        shipment.setStatus(ShipmentStatus.PLANNED);
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));

        // When & Then
        assertThrows(InvalidOperationException.class,
                () -> shipmentService.markAsDelivered(1L));

        verify(shipmentRepository, never()).save(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // UPDATE & DELETE TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void updatePlannedDate_Success() {
        // Given
        LocalDateTime newDate = LocalDateTime.now().plusDays(5);
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(shipment);

        // When
        ApiResponse<ShipmentResponseDto> response =
                shipmentService.updatePlannedDate(1L, newDate);

        // Then
        assertNotNull(response);
        assertEquals("Shipment planned date updated", response.getMessage());
        verify(shipmentRepository).save(any(Shipment.class));
    }

    @Test
    void updatePlannedDate_Delivered_ThrowsException() {
        // Given
        shipment.setStatus(ShipmentStatus.DELIVERED);
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));

        // When & Then
        assertThrows(InvalidOperationException.class,
                () -> shipmentService.updatePlannedDate(1L, LocalDateTime.now()));

        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void deleteShipment_Planned_Success() {
        // Given
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));
        doNothing().when(shipmentRepository).deleteById(1L);

        // When
        ApiResponse<Void> response = shipmentService.deleteShipment(1L);

        // Then
        assertNotNull(response);
        assertEquals("Shipment deleted successfully", response.getMessage());
        verify(shipmentRepository).deleteById(1L);
    }

    @Test
    void deleteShipment_NotPlanned_ThrowsException() {
        // Given
        shipment.setStatus(ShipmentStatus.IN_TRANSIT);
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));

        // When & Then
        assertThrows(InvalidOperationException.class,
                () -> shipmentService.deleteShipment(1L));

        verify(shipmentRepository, never()).deleteById(anyLong());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // ASSIGN CARRIER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void assignCarrier_Success() {
        // Given
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));
        when(carrierRepository.findById(1L)).thenReturn(Optional.of(carrier));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(shipment);
        doNothing().when(carrierService).incrementDailyShipments(1L);

        // When
        ApiResponse<ShipmentResponseDto> response =
                shipmentService.assignCarrier(1L, 1L);

        // Then
        assertNotNull(response);
        assertTrue(response.getMessage().contains("assigned"));
        verify(shipmentRepository).save(any(Shipment.class));
        verify(carrierService).incrementDailyShipments(1L);
    }

    @Test
    void assignCarrier_NotPlanned_ThrowsException() {
        // Given
        shipment.setStatus(ShipmentStatus.IN_TRANSIT);
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));

        // When & Then
        assertThrows(InvalidOperationException.class,
                () -> shipmentService.assignCarrier(1L, 1L));

        verify(carrierRepository, never()).findById(anyLong());
    }

    @Test
    void assignCarrier_CarrierNotActive_ThrowsException() {
        // Given
        carrier.setStatus(CarrierStatus.SUSPENDED);
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));
        when(carrierRepository.findById(1L)).thenReturn(Optional.of(carrier));

        // When & Then
        assertThrows(InvalidOperationException.class,
                () -> shipmentService.assignCarrier(1L, 1L));

        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void assignCarrier_MaxCapacityReached_ThrowsException() {
        // Given
        carrier.setCurrentDailyShipments(100); // Equal to max capacity
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));
        when(carrierRepository.findById(1L)).thenReturn(Optional.of(carrier));

        // When & Then
        assertThrows(InvalidOperationException.class,
                () -> shipmentService.assignCarrier(1L, 1L));

        verify(shipmentRepository, never()).save(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // ASSIGN MULTIPLE SHIPMENTS TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void assignMultipleShipments_Success() {
        // Given
        Shipment shipment2 = Shipment.builder()
                .id(2L)
                .status(ShipmentStatus.PLANNED)
                .build();

        List<Long> shipmentIds = Arrays.asList(1L, 2L);

        when(carrierRepository.findById(1L)).thenReturn(Optional.of(carrier));
        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.findById(2L)).thenReturn(Optional.of(shipment2));
        when(shipmentRepository.save(any(Shipment.class)))
                .thenReturn(shipment)
                .thenReturn(shipment2);
        when(carrierRepository.save(any(Carrier.class))).thenReturn(carrier);

        // When
        ApiResponse<List<ShipmentResponseDto>> response =
                shipmentService.assignMultipleShipments(1L, shipmentIds);

        // Then
        assertNotNull(response);
        assertTrue(response.getMessage().contains("2 shipments"));
        assertEquals(2, response.getData().size());

        verify(shipmentRepository, times(2)).save(any(Shipment.class));
        verify(carrierRepository).save(any(Carrier.class));
    }

    @Test
    void assignMultipleShipments_ExceedsCapacity_ThrowsException() {
        // Given
        carrier.setCurrentDailyShipments(99); // Only 1 slot available
        List<Long> shipmentIds = Arrays.asList(1L, 2L); // Trying to assign 2

        when(carrierRepository.findById(1L)).thenReturn(Optional.of(carrier));

        // When & Then
        assertThrows(InvalidOperationException.class,
                () -> shipmentService.assignMultipleShipments(1L, shipmentIds));

        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void assignMultipleShipments_CarrierNotActive_ThrowsException() {
        // Given
        carrier.setStatus(CarrierStatus.SUSPENDED);
        List<Long> shipmentIds = Arrays.asList(1L);

        when(carrierRepository.findById(1L)).thenReturn(Optional.of(carrier));

        // When & Then
        assertThrows(InvalidOperationException.class,
                () -> shipmentService.assignMultipleShipments(1L, shipmentIds));

        verify(shipmentRepository, never()).findById(anyLong());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // GET BY CARRIER & COUNT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getShipmentsByCarrier_Success() {
        // Given
        when(carrierRepository.existsById(1L)).thenReturn(true);
        when(shipmentRepository.findByCarrierId(1L)).thenReturn(Arrays.asList(shipment));

        // When
        ApiResponse<List<ShipmentResponseDto>> response =
                shipmentService.getShipmentsByCarrier(1L);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getData().size());
        verify(carrierRepository).existsById(1L);
        verify(shipmentRepository).findByCarrierId(1L);
    }

    @Test
    void getShipmentsByCarrier_CarrierNotFound_ThrowsException() {
        // Given
        when(carrierRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> shipmentService.getShipmentsByCarrier(999L));

        verify(shipmentRepository, never()).findByCarrierId(anyLong());
    }

    @Test
    void countShipments_Success() {
        // Given
        when(shipmentRepository.count()).thenReturn(50L);

        // When
        ApiResponse<Long> response = shipmentService.countShipments();

        // Then
        assertNotNull(response);
        assertEquals(50L, response.getData());
        verify(shipmentRepository).count();
    }

    @Test
    void countShipmentsByStatus_Success() {
        // Given
        when(shipmentRepository.findByStatus(ShipmentStatus.IN_TRANSIT))
                .thenReturn(Arrays.asList(shipment, shipment));

        // When
        ApiResponse<Long> response =
                shipmentService.countShipmentsByStatus(ShipmentStatus.IN_TRANSIT);

        // Then
        assertNotNull(response);
        assertEquals(2L, response.getData());
        verify(shipmentRepository).findByStatus(ShipmentStatus.IN_TRANSIT);
    }
}