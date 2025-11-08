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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesOrderReserveReleaseTest {

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

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .name("Dell Laptop")
                .sku("PROD-001")
                .category("Electronics")
                .originalPrice(15000L)           // ✅ Fixed
                .profite(BigDecimal.valueOf(0))  // ✅ Fixed
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
                .status(OrderStatus.CREATED)
                .build();

        orderLine.setSalesOrder(salesOrder);
        salesOrder.setOrderLines(new ArrayList<>(List.of(orderLine)));
    }

    @Test
    void testReserveStock_QuantityReservedCorrectly() {
        // Given
        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(salesOrder));
        when(inventoryRepository.findByProductId(1L)).thenReturn(List.of(inventory));
        when(inventoryRepository.findByWarehouseIdAndProductId(1L, 1L))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(salesOrder);

        // When
        salesOrderService.reserveStock(1L);

        // Then
        assertEquals(50, inventory.getQtyReserved());
        assertEquals(100, inventory.getQtyOnHand());
        assertEquals(OrderStatus.RESERVED, salesOrder.getStatus());

        verify(inventoryRepository, atLeastOnce()).save(inventory);
    }

    @Test
    void testCancelOrder_StockReleasedCorrectly() {
        // Given
        inventory.setQtyReserved(50);
        salesOrder.setStatus(OrderStatus.RESERVED);

        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(salesOrder));
        when(inventoryRepository.findByWarehouseIdAndProductId(1L, 1L))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(salesOrder);

        // When
        salesOrderService.cancelOrder(1L);

        // Then
        assertEquals(0, inventory.getQtyReserved());
        assertEquals(100, inventory.getQtyOnHand());
        assertEquals(OrderStatus.CANCELED, salesOrder.getStatus());

        verify(inventoryRepository, times(1)).save(inventory);
    }

    @Test
    void testMultipleReservations_QuantityAccumulates() {
        // Given
        inventory.setQtyReserved(30);

        SalesOrderLine orderLine2 = SalesOrderLine.builder()
                .id(2L)
                .product(product)
                .warehouse(warehouse)
                .quantity(20)
                .unitPrice(BigDecimal.valueOf(15000.0))
                .build();

        SalesOrder salesOrder2 = SalesOrder.builder()
                .id(2L)
                .status(OrderStatus.CREATED)
                .build();

        orderLine2.setSalesOrder(salesOrder2);
        salesOrder2.setOrderLines(new ArrayList<>(List.of(orderLine2)));

        when(salesOrderRepository.findById(2L)).thenReturn(Optional.of(salesOrder2));
        when(inventoryRepository.findByProductId(1L)).thenReturn(List.of(inventory));
        when(inventoryRepository.findByWarehouseIdAndProductId(1L, 1L))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(salesOrder2);

        // When
        salesOrderService.reserveStock(2L);

        // Then
        assertEquals(50, inventory.getQtyReserved());
    }
}