package org.project.digital_logistics.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.digital_logistics.exception.InvalidOperationException;
import org.project.digital_logistics.model.*;
import org.project.digital_logistics.model.enums.OrderStatus;
import org.project.digital_logistics.repository.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesOrderStatusTransitionTest {

    @Mock
    private SalesOrderRepository salesOrderRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryMovementService movementService;

    @Mock
    private ShipmentService shipmentService;

    @InjectMocks
    private SalesOrderService salesOrderService;

    private SalesOrder salesOrder;
    private Inventory inventory;
    private Product product;
    private Warehouse warehouse;
    private Client client;

    @BeforeEach
    void setUp() {
        // ✅ Client
        client = Client.builder()
                .id(1L)
                .name("Test Client")
                .email("client@test.com")
                .phoneNumber("0612345678")
                .address("Casablanca, Morocco")
                .build();

        product = Product.builder()
                .id(1L)
                .name("Dell Laptop")
                .sku("PROD-001")
                .category("Electronics")
                .originalPrice(15000L)
                .profite(BigDecimal.valueOf(0))
                .active(true)
                .build();

        warehouse = Warehouse.builder()
                .id(1L)
                .code("WH-001")
                .name("Central Warehouse")
                .build();

        inventory = Inventory.builder()
                .id(1L)
                .product(product)
                .warehouse(warehouse)
                .qtyOnHand(100)
                .qtyReserved(0)
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
                .client(client)  // ✅ Fixed
                .status(OrderStatus.CREATED)
                .build();

        orderLine.setSalesOrder(salesOrder);
        salesOrder.setOrderLines(new ArrayList<>(List.of(orderLine)));
    }

    @Test
    void testTransition_CREATED_to_RESERVED() {
        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(salesOrder));
        when(inventoryRepository.findByProductId(1L)).thenReturn(List.of(inventory));
        when(inventoryRepository.findByWarehouseIdAndProductId(1L, 1L))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(salesOrder);

        salesOrderService.reserveStock(1L);

        assertEquals(OrderStatus.RESERVED, salesOrder.getStatus());
        assertNotNull(salesOrder.getReservedAt());
    }

    @Test
    void testTransition_RESERVED_to_SHIPPED() {
        salesOrder.setStatus(OrderStatus.RESERVED);
        inventory.setQtyReserved(50);

        Shipment mockShipment = Shipment.builder()
                .id(1L)
                .trackingNumber("TRACK-123")
                .build();

        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(salesOrder));
        when(inventoryRepository.findByWarehouseIdAndProductId(1L, 1L))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(salesOrder);
        when(shipmentService.autoCreateShipment(any())).thenReturn(mockShipment);

        salesOrderService.shipOrder(1L);

        assertEquals(OrderStatus.SHIPPED, salesOrder.getStatus());
        assertNotNull(salesOrder.getShippedAt());
        assertEquals(50, inventory.getQtyOnHand());
        assertEquals(0, inventory.getQtyReserved());
    }

    @Test
    void testTransition_SHIPPED_to_DELIVERED() {
        salesOrder.setStatus(OrderStatus.SHIPPED);

        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(salesOrder));
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(salesOrder);

        salesOrderService.deliverOrder(1L);

        assertEquals(OrderStatus.DELIVERED, salesOrder.getStatus());
        assertNotNull(salesOrder.getDeliveredAt());
    }

    @Test
    void testTransition_CREATED_to_CANCELED() {
        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(salesOrder));
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(salesOrder);

        salesOrderService.cancelOrder(1L);

        assertEquals(OrderStatus.CANCELED, salesOrder.getStatus());
    }

    @Test
    void testTransition_RESERVED_to_CANCELED() {
        salesOrder.setStatus(OrderStatus.RESERVED);
        inventory.setQtyReserved(50);

        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(salesOrder));
        when(inventoryRepository.findByWarehouseIdAndProductId(1L, 1L))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(salesOrder);

        salesOrderService.cancelOrder(1L);

        assertEquals(OrderStatus.CANCELED, salesOrder.getStatus());
        assertEquals(0, inventory.getQtyReserved());
    }

    @Test
    void testInvalidTransition_CREATED_to_SHIPPED() {
        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(salesOrder));

        InvalidOperationException exception = assertThrows(
                InvalidOperationException.class,
                () -> salesOrderService.shipOrder(1L)
        );

        assertTrue(exception.getMessage().contains("RESERVED"));
    }

    @Test
    void testInvalidTransition_SHIPPED_to_CANCELED() {
        salesOrder.setStatus(OrderStatus.SHIPPED);

        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(salesOrder));

        InvalidOperationException exception = assertThrows(
                InvalidOperationException.class,
                () -> salesOrderService.cancelOrder(1L)
        );

        assertTrue(exception.getMessage().contains("Cannot cancel"));
    }

    @Test
    void testInvalidTransition_DELIVERED_to_CANCELED() {
        salesOrder.setStatus(OrderStatus.DELIVERED);

        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(salesOrder));

        InvalidOperationException exception = assertThrows(
                InvalidOperationException.class,
                () -> salesOrderService.cancelOrder(1L)
        );

        assertTrue(exception.getMessage().contains("Cannot cancel"));
    }

    @Test
    void testInvalidTransition_CREATED_to_DELIVERED() {
        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(salesOrder));

        InvalidOperationException exception = assertThrows(
                InvalidOperationException.class,
                () -> salesOrderService.deliverOrder(1L)
        );

        assertTrue(exception.getMessage().contains("SHIPPED"));
    }
}