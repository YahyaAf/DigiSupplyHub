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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesOrderBackorderTest {

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
        Client client = Client.builder()
                .id(1L)
                .name("Test Client")
                .build();

        Product product = Product.builder()
                .id(1L)
                .name("Dell Laptop")
                .sku("PROD-001")
                .originalPrice(15000L)
                .profite(BigDecimal.valueOf(0))
                .active(true)
                .build();

        Warehouse warehouse = Warehouse.builder()
                .id(1L)
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
                .quantity(150)
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
    void testBackorder_PartialStockRejected() {
        // Given
        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(salesOrder));
        when(inventoryRepository.findByProductId(1L)).thenReturn(List.of(inventory));

        // When & Then
        InsufficientStockException exception = assertThrows(
                InsufficientStockException.class,
                () -> salesOrderService.reserveStock(1L)
        );

        assertNotNull(exception);

        String message = exception.getMessage();
        assertTrue(
                message.contains("Insufficient") ||
                        message.contains("Dell Laptop") ||
                        message.contains("150") ||
                        message.contains("100"),
                "Message doit contenir info sur stock insuffisant. Message: " + message
        );

        assertEquals(0, inventory.getQtyReserved());

        assertEquals(OrderStatus.CREATED, salesOrder.getStatus());
    }
}