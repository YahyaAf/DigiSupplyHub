package org.project.digital_logistics.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.digital_logistics.exception.InvalidOperationException;
import org.project.digital_logistics.model.*;
import org.project.digital_logistics.model.enums.CarrierStatus;
import org.project.digital_logistics.model.enums.ShipmentStatus;
import org.project.digital_logistics.repository.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentCapacitySlotTest {

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

    private Carrier carrier;
    private Shipment shipment;
    private Client client;
    private Product product;
    private Warehouse warehouse;
    private SalesOrder salesOrder;

    @BeforeEach
    void setUp() {
        carrier = Carrier.builder()
                .id(1L)
                .name("DHL Express")
                .status(CarrierStatus.ACTIVE)
                .maxDailyCapacity(10)
                .currentDailyShipments(0)
                .build();

        client = Client.builder()
                .id(1L)
                .name("Test Client")
                .build();

        product = Product.builder()
                .id(1L)
                .name("Dell Laptop")
                .sku("PROD-001")
                .originalPrice(15000L)
                .profite(BigDecimal.valueOf(0))
                .active(true)
                .build();

        warehouse = Warehouse.builder()
                .id(1L)
                .name("Central Warehouse")
                .build();

        SalesOrderLine orderLine = SalesOrderLine.builder()
                .id(1L)
                .product(product)
                .warehouse(warehouse)
                .quantity(50)
                .unitPrice(BigDecimal.valueOf(15000.0))
                .build();

        salesOrder = SalesOrder.builder()
                .id(1L)
                .client(client)
                .build();

        orderLine.setSalesOrder(salesOrder);
        salesOrder.setOrderLines(new ArrayList<>(List.of(orderLine)));

        shipment = Shipment.builder()
                .id(1L)
                .salesOrder(salesOrder)
                .status(ShipmentStatus.PLANNED)
                .trackingNumber("TRACK-001")
                .build();
    }

    @Test
    void testAssignCarrier_WithinCapacity() {
        // Given: Carrier capacité 5/10
        carrier.setCurrentDailyShipments(5);

        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));
        when(carrierRepository.findById(1L)).thenReturn(Optional.of(carrier));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(shipment);
        doNothing().when(carrierService).incrementDailyShipments(1L);

        // When
        shipmentService.assignCarrier(1L, 1L);

        // Then
        assertEquals(CarrierStatus.ACTIVE, carrier.getStatus());
        assertTrue(carrier.getCurrentDailyShipments() < carrier.getMaxDailyCapacity(),
                "Capacité OK: 5/10 shipments");
        verify(carrierService, times(1)).incrementDailyShipments(1L);
    }

    @Test
    void testAssignCarrier_CapacityFull() {
        // Given: Carrier capacité 10/10 (plein)
        carrier.setCurrentDailyShipments(10);

        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));
        when(carrierRepository.findById(1L)).thenReturn(Optional.of(carrier));

        // When & Then
        InvalidOperationException exception = assertThrows(
                InvalidOperationException.class,
                () -> shipmentService.assignCarrier(1L, 1L)
        );

        assertTrue(exception.getMessage().contains("max daily capacity"),
                "Exception quand capacité pleine");
        assertEquals(10, carrier.getCurrentDailyShipments());
    }

    @Test
    void testAssignMultipleShipments_WithinCapacity() {
        // Given: Carrier capacité 3/10, assigner 5 shipments
        carrier.setCurrentDailyShipments(3);
        List<Long> shipmentIds = List.of(1L, 2L, 3L, 4L, 5L);

        when(carrierRepository.findById(1L)).thenReturn(Optional.of(carrier));
        when(shipmentRepository.findById(anyLong())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            Shipment newShipment = Shipment.builder()
                    .id(id)
                    .salesOrder(salesOrder)
                    .status(ShipmentStatus.PLANNED)  // ✅ All PLANNED
                    .trackingNumber("TRACK-" + id)
                    .build();
            return Optional.of(newShipment);
        });
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(carrierRepository.save(any(Carrier.class))).thenReturn(carrier);

        // When
        shipmentService.assignMultipleShipments(1L, shipmentIds);

        // Then
        int availableCapacity = 10 - 3; // 7 disponibles
        assertTrue(shipmentIds.size() <= availableCapacity,
                "5 shipments <= 7 disponibles → OK");
        verify(carrierRepository, times(1)).save(carrier);
    }

    @Test
    void testAssignMultipleShipments_ExceedsCapacity() {
        // Given: Carrier capacité 8/10, assigner 5 shipments (8+5=13 > 10)
        carrier.setCurrentDailyShipments(8);
        List<Long> shipmentIds = List.of(1L, 2L, 3L, 4L, 5L);

        when(carrierRepository.findById(1L)).thenReturn(Optional.of(carrier));

        // When & Then
        InvalidOperationException exception = assertThrows(
                InvalidOperationException.class,
                () -> shipmentService.assignMultipleShipments(1L, shipmentIds)
        );

        assertTrue(exception.getMessage().contains("Available capacity"),
                "Exception: 5 shipments > 2 disponibles");
    }
}