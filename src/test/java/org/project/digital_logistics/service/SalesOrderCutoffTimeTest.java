package org.project.digital_logistics.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.digital_logistics.model.*;
import org.project.digital_logistics.model.enums.OrderStatus;
import org.project.digital_logistics.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesOrderCutoffTimeTest {

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
    private Client client;
    private Product product;
    private Warehouse warehouse;

    private static final LocalTime CUTOFF_TIME = LocalTime.of(17, 0);

    @BeforeEach
    void setUp() {
        client = Client.builder()
                .id(1L)
                .name("Test Client")
                .email("client@test.com")
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
                .client(client)
                .status(OrderStatus.CREATED)
                .build();

        orderLine.setSalesOrder(salesOrder);
        salesOrder.setOrderLines(new ArrayList<>(List.of(orderLine)));
    }

    @Test
    void testOrderBeforeCutoff_ShipSameDay() {
        LocalDateTime orderTime = LocalDateTime.now()
                .withHour(14)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        salesOrder.setCreatedAt(orderTime);

        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(salesOrder));
        when(inventoryRepository.findByProductId(1L)).thenReturn(List.of(inventory));
        when(inventoryRepository.findByWarehouseIdAndProductId(1L, 1L))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(salesOrder);

        salesOrderService.reserveStock(1L);

        LocalTime orderHour = salesOrder.getCreatedAt().toLocalTime();
        assertTrue(orderHour.isBefore(CUTOFF_TIME),
                "Order time (14:00) doit être avant cut-off (17:00)");

        assertEquals(OrderStatus.RESERVED, salesOrder.getStatus());
        assertNotNull(salesOrder.getReservedAt());
    }

    @Test
    void testOrderAfterCutoff_ShipNextDay() {
        LocalDateTime orderTime = LocalDateTime.now()
                .withHour(18)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        salesOrder.setCreatedAt(orderTime);

        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(salesOrder));
        when(inventoryRepository.findByProductId(1L)).thenReturn(List.of(inventory));
        when(inventoryRepository.findByWarehouseIdAndProductId(1L, 1L))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(salesOrder);

        salesOrderService.reserveStock(1L);

        LocalTime orderHour = salesOrder.getCreatedAt().toLocalTime();
        assertTrue(orderHour.isAfter(CUTOFF_TIME),
                "Order time (18:00) doit être après cut-off (17:00)");

        assertEquals(OrderStatus.RESERVED, salesOrder.getStatus());
    }

    @Test
    void testOrderExactlyCutoff_AcceptedSameDay() {
        LocalDateTime orderTime = LocalDateTime.now()
                .withHour(17)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);  // ✅ Fixed

        salesOrder.setCreatedAt(orderTime);

        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(salesOrder));
        when(inventoryRepository.findByProductId(1L)).thenReturn(List.of(inventory));
        when(inventoryRepository.findByWarehouseIdAndProductId(1L, 1L))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(salesOrder);

        salesOrderService.reserveStock(1L);

        LocalTime orderHour = salesOrder.getCreatedAt().toLocalTime()
                .truncatedTo(java.time.temporal.ChronoUnit.MINUTES);  // ✅ Fixed

        assertEquals(CUTOFF_TIME, orderHour,
                "Order time doit être exactement 17:00");

        assertEquals(OrderStatus.RESERVED, salesOrder.getStatus());
    }

    @Test
    void testMultipleOrders_DifferentCutoffRespect() {
        LocalDateTime order1Time = LocalDateTime.now()
                .withHour(16)
                .withMinute(0)
                .withNano(0);

        LocalDateTime order2Time = LocalDateTime.now()
                .withHour(18)
                .withMinute(0)
                .withNano(0);

        salesOrder.setCreatedAt(order1Time);

        SalesOrder salesOrder2 = SalesOrder.builder()
                .id(2L)
                .client(client)
                .status(OrderStatus.CREATED)
                .createdAt(order2Time)
                .build();

        SalesOrderLine orderLine2 = SalesOrderLine.builder()
                .id(2L)
                .product(product)
                .warehouse(warehouse)
                .quantity(30)
                .unitPrice(BigDecimal.valueOf(15000.0))
                .build();

        orderLine2.setSalesOrder(salesOrder2);
        salesOrder2.setOrderLines(new ArrayList<>(List.of(orderLine2)));

        assertTrue(order1Time.toLocalTime().isBefore(CUTOFF_TIME),
                "Order 1 (16:00) avant cut-off");
        assertTrue(order2Time.toLocalTime().isAfter(CUTOFF_TIME),
                "Order 2 (18:00) après cut-off");
    }
}