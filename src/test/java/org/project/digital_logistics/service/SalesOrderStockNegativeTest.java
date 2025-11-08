package org.project.digital_logistics.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.digital_logistics.exception.InsufficientStockException;
import org.project.digital_logistics.model.*;
import org.project.digital_logistics.model.enums.OrderStatus;
import org.project.digital_logistics.repository.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesOrderStockNegativeTest {

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

    @BeforeEach
    void setUp() {
        Product product = Product.builder()
                .id(1L)
                .name("Dell Laptop")
                .build();

        Warehouse warehouse = Warehouse.builder()
                .id(1L)
                .name("Central Warehouse")
                .build();

        inventory = Inventory.builder()
                .id(1L)
                .product(product)
                .warehouse(warehouse)
                .qtyOnHand(100)      // Stock: 100
                .qtyReserved(0)      // Réservé: 0
                .build();

        SalesOrderLine orderLine = SalesOrderLine.builder()
                .id(1L)
                .product(product)
                .warehouse(warehouse)
                .quantity(150)       // Demande: 150 (plus que stock!)
                .build();

        salesOrder = SalesOrder.builder()
                .id(1L)
                .status(OrderStatus.CREATED)
                .build();

        orderLine.setSalesOrder(salesOrder);
        salesOrder.setOrderLines(List.of(orderLine));
    }

    @Test
    void testStockNegatifRejected() {
        // Given
        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(salesOrder));
        when(inventoryRepository.findByProductId(1L)).thenReturn(List.of(inventory));

        // When & Then
        InsufficientStockException exception = assertThrows(
                InsufficientStockException.class,
                () -> salesOrderService.reserveStock(1L)
        );

        assertTrue(exception.getMessage().contains("Dell Laptop"));
        verify(inventoryRepository, never()).save(any());
    }
}