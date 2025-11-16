package org.project.digital_logistics.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.salesorder.SalesOrderLineDto;
import org.project.digital_logistics.dto.salesorder.SalesOrderRequestDto;
import org.project.digital_logistics.dto.salesorder.SalesOrderResponseDto;
import org.project.digital_logistics.exception.InsufficientStockException;
import org.project.digital_logistics.exception.InvalidOperationException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.model.*;
import org.project.digital_logistics.model.enums.MovementType;
import org.project.digital_logistics.model.enums.OrderStatus;
import org.project.digital_logistics.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesOrderServiceTest {

    @Mock
    private SalesOrderRepository salesOrderRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryMovementService movementService;

    @Mock
    private ShipmentService shipmentService;

    @InjectMocks
    private SalesOrderService salesOrderService;

    private Client client;
    private Product product;
    private Warehouse warehouse;
    private Inventory inventory;
    private SalesOrder salesOrder;
    private SalesOrderRequestDto requestDto;
    private SalesOrderLineDto lineDto;
    private Shipment shipment;

    @BeforeEach
    void setUp() {
        // Setup Client
        client = Client.builder()
                .id(1L)
                .name("Test Client")
                .email("client@test.com")
                .phoneNumber("0612345678")
                .address("123 Test Street")
                .active(true)
                .build();

        // Setup Product
        product = Product.builder()
                .id(1L)
                .sku("PROD-001")
                .name("Test Product")
                .category("Electronics")
                .active(true)
                .originalPrice(10000L)
                .profite(BigDecimal.valueOf(1000))
                .build();

        // Setup Warehouse
        warehouse = Warehouse.builder()
                .id(1L)
                .code("WH-001")
                .name("Main Warehouse")
                .capacity(10000)
                .active(true)
                .build();

        // ✅ Setup Carrier (Object, not String)
        Carrier carrier = Carrier.builder()
                .id(1L)
                .code("DHL-001")
                .name("DHL Express")
                .contactEmail("contact@dhl.com")
                .contactPhone("0612345678")
                .baseShippingRate(BigDecimal.valueOf(50))
                .maxDailyCapacity(100)
                .build();

        // Setup Inventory
        inventory = Inventory.builder()
                .id(1L)
                .warehouse(warehouse)
                .product(product)
                .qtyOnHand(100)
                .qtyReserved(0)
                .build();

        // Setup SalesOrderLineDto
        lineDto = SalesOrderLineDto.builder()
                .productId(1L)
                .quantity(10)
                .unitPrice(BigDecimal.valueOf(11000))
                .build();

        // Setup SalesOrderRequestDto
        requestDto = SalesOrderRequestDto.builder()
                .orderLines(Arrays.asList(lineDto))
                .build();

        // Setup SalesOrder
        salesOrder = SalesOrder.builder()
                .id(1L)
                .client(client)
                .status(OrderStatus.CREATED)
                .build();

        SalesOrderLine line = SalesOrderLine.builder()
                .id(1L)
                .salesOrder(salesOrder)
                .product(product)
                .warehouse(warehouse)
                .quantity(10)
                .unitPrice(BigDecimal.valueOf(11000))
                .build();

        salesOrder.addOrderLine(line);

        // ✅ Setup Shipment (with Carrier object)
        shipment = Shipment.builder()
                .id(1L)
                .salesOrder(salesOrder)
                .trackingNumber("TRACK-12345")
                .carrier(carrier)  // ✅ Carrier object, not String
                .shippedDate(LocalDateTime.now())
                .build();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CREATE SALES ORDER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void createSalesOrder_WithSufficientStock_ReservesAutomatically() {
        // Given
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductId(1L)).thenReturn(Arrays.asList(inventory));
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(salesOrder);

        // When
        ApiResponse<SalesOrderResponseDto> response =
                salesOrderService.createSalesOrder(requestDto, 1L);

        // Then
        assertNotNull(response);
        assertTrue(response.getMessage().contains("created"));
        assertNotNull(response.getData());

        verify(clientRepository).findById(1L);
        verify(productRepository, atLeastOnce()).findById(1L);
        verify(inventoryRepository, atLeastOnce()).findByProductId(1L);
        verify(salesOrderRepository, atLeastOnce()).save(any(SalesOrder.class));
    }

    @Test
    void createSalesOrder_WithInsufficientStock_CreatesWithoutReservation() {
        // Given
        inventory.setQtyOnHand(5); // Less than requested (10)

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductId(1L)).thenReturn(Arrays.asList(inventory));
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(salesOrder);

        // When
        ApiResponse<SalesOrderResponseDto> response =
                salesOrderService.createSalesOrder(requestDto, 1L);

        // Then
        assertNotNull(response);
        assertTrue(response.getMessage().contains("not reserved yet") ||
                response.getMessage().contains("Insufficient stock"));

        verify(salesOrderRepository, atLeastOnce()).save(any(SalesOrder.class));
    }

    @Test
    void createSalesOrder_ClientNotFound_ThrowsException() {
        // Given
        when(clientRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> salesOrderService.createSalesOrder(requestDto, 999L));

        verify(clientRepository).findById(999L);
        verify(salesOrderRepository, never()).save(any());
    }

    @Test
    void createSalesOrder_ProductNotFound_ThrowsException() {
        // Given
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());
        lineDto.setProductId(999L);

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> salesOrderService.createSalesOrder(requestDto, 1L));

        verify(clientRepository).findById(1L);
        verify(productRepository).findById(999L);
        verify(salesOrderRepository, never()).save(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // RESERVE STOCK TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void reserveStock_Success() {
        // Given
        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(salesOrder));
        when(inventoryRepository.findByProductId(1L)).thenReturn(Arrays.asList(inventory));
        when(inventoryRepository.findByWarehouseIdAndProductId(1L, 1L))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(salesOrder);

        // When
        ApiResponse<SalesOrderResponseDto> response = salesOrderService.reserveStock(1L);

        // Then
        assertNotNull(response);
        assertTrue(response.getMessage().contains("reserved successfully"));

        verify(inventoryRepository, atLeastOnce()).save(any(Inventory.class));
        verify(salesOrderRepository).save(any(SalesOrder.class));
    }

    @Test
    void reserveStock_NotCreatedStatus_ThrowsException() {
        // Given
        salesOrder.setStatus(OrderStatus.RESERVED);
        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(salesOrder));

        // When & Then
        assertThrows(InvalidOperationException.class,
                () -> salesOrderService.reserveStock(1L));

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void reserveStock_InsufficientStock_ThrowsException() {
        // Given
        inventory.setQtyOnHand(5); // Less than line quantity (10)

        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(salesOrder));
        when(inventoryRepository.findByProductId(1L)).thenReturn(Arrays.asList(inventory));

        // When & Then
        assertThrows(InsufficientStockException.class,
                () -> salesOrderService.reserveStock(1L));

        verify(inventoryRepository, never()).save(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // SHIP ORDER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void shipOrder_Success() {
        // Given
        salesOrder.setStatus(OrderStatus.RESERVED);
        inventory.setQtyReserved(10);

        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(salesOrder));
        when(inventoryRepository.findByWarehouseIdAndProductId(1L, 1L))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(salesOrder);
        when(shipmentService.autoCreateShipment(any(SalesOrder.class))).thenReturn(shipment);
        doNothing().when(movementService).recordMovement(
                anyLong(), any(MovementType.class), anyInt(), anyString(), anyString());

        // When
        ApiResponse<SalesOrderResponseDto> response = salesOrderService.shipOrder(1L);

        // Then
        assertNotNull(response);
        assertTrue(response.getMessage().contains("shipped successfully"));
        assertTrue(response.getMessage().contains("TRACK-12345"));

        verify(inventoryRepository).save(any(Inventory.class));
        verify(movementService).recordMovement(
                eq(1L), eq(MovementType.OUTBOUND), eq(10), eq("SO-1"), anyString());
        verify(shipmentService).autoCreateShipment(any(SalesOrder.class));

        ArgumentCaptor<SalesOrder> captor = ArgumentCaptor.forClass(SalesOrder.class);
        verify(salesOrderRepository).save(captor.capture());

        SalesOrder savedOrder = captor.getValue();
        assertEquals(OrderStatus.SHIPPED, savedOrder.getStatus());
        assertNotNull(savedOrder.getShippedAt());
    }

    @Test
    void shipOrder_NotReservedStatus_ThrowsException() {
        // Given
        salesOrder.setStatus(OrderStatus.CREATED);
        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(salesOrder));

        // When & Then
        assertThrows(InvalidOperationException.class,
                () -> salesOrderService.shipOrder(1L));

        verify(inventoryRepository, never()).save(any());
        verify(shipmentService, never()).autoCreateShipment(any());
    }

    @Test
    void shipOrder_InventoryNotFound_ThrowsException() {
        // Given
        salesOrder.setStatus(OrderStatus.RESERVED);
        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(salesOrder));
        when(inventoryRepository.findByWarehouseIdAndProductId(1L, 1L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> salesOrderService.shipOrder(1L));

        verify(movementService, never()).recordMovement(
                anyLong(), any(), anyInt(), anyString(), anyString());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // DELIVER ORDER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void deliverOrder_Success() {
        // Given
        salesOrder.setStatus(OrderStatus.SHIPPED);
        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(salesOrder));
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(salesOrder);

        // When
        ApiResponse<SalesOrderResponseDto> response = salesOrderService.deliverOrder(1L);

        // Then
        assertNotNull(response);
        assertEquals("Order delivered successfully", response.getMessage());

        ArgumentCaptor<SalesOrder> captor = ArgumentCaptor.forClass(SalesOrder.class);
        verify(salesOrderRepository).save(captor.capture());

        SalesOrder savedOrder = captor.getValue();
        assertEquals(OrderStatus.DELIVERED, savedOrder.getStatus());
        assertNotNull(savedOrder.getDeliveredAt());
    }

    @Test
    void deliverOrder_NotShippedStatus_ThrowsException() {
        // Given
        salesOrder.setStatus(OrderStatus.RESERVED);
        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(salesOrder));

        // When & Then
        assertThrows(InvalidOperationException.class,
                () -> salesOrderService.deliverOrder(1L));

        verify(salesOrderRepository, never()).save(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CANCEL ORDER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void cancelOrder_CreatedStatus_Success() {
        // Given
        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(salesOrder));
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(salesOrder);

        // When
        ApiResponse<SalesOrderResponseDto> response = salesOrderService.cancelOrder(1L);

        // Then
        assertNotNull(response);
        assertEquals("Order canceled successfully", response.getMessage());

        ArgumentCaptor<SalesOrder> captor = ArgumentCaptor.forClass(SalesOrder.class);
        verify(salesOrderRepository).save(captor.capture());

        assertEquals(OrderStatus.CANCELED, captor.getValue().getStatus());
    }

    @Test
    void cancelOrder_ReservedStatus_ReleasesStock() {
        // Given
        salesOrder.setStatus(OrderStatus.RESERVED);
        inventory.setQtyReserved(10);

        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(salesOrder));
        when(inventoryRepository.findByWarehouseIdAndProductId(1L, 1L))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(salesOrder);

        // When
        ApiResponse<SalesOrderResponseDto> response = salesOrderService.cancelOrder(1L);

        // Then
        assertNotNull(response);
        verify(inventoryRepository).save(any(Inventory.class));
        verify(salesOrderRepository).save(any(SalesOrder.class));
    }

    @Test
    void cancelOrder_ShippedStatus_ThrowsException() {
        // Given
        salesOrder.setStatus(OrderStatus.SHIPPED);
        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(salesOrder));

        // When & Then
        assertThrows(InvalidOperationException.class,
                () -> salesOrderService.cancelOrder(1L));

        verify(salesOrderRepository, never()).save(any());
    }

    @Test
    void cancelOrder_DeliveredStatus_ThrowsException() {
        // Given
        salesOrder.setStatus(OrderStatus.DELIVERED);
        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(salesOrder));

        // When & Then
        assertThrows(InvalidOperationException.class,
                () -> salesOrderService.cancelOrder(1L));

        verify(salesOrderRepository, never()).save(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // GET OPERATIONS TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getSalesOrderById_Success() {
        // Given
        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(salesOrder));

        // When
        ApiResponse<SalesOrderResponseDto> response = salesOrderService.getSalesOrderById(1L);

        // Then
        assertNotNull(response);
        assertEquals("Sales order retrieved successfully", response.getMessage());
        assertEquals(1L, response.getData().getId());
        verify(salesOrderRepository).findById(1L);
    }

    @Test
    void getSalesOrderById_NotFound_ThrowsException() {
        // Given
        when(salesOrderRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> salesOrderService.getSalesOrderById(999L));

        verify(salesOrderRepository).findById(999L);
    }

    @Test
    void getAllSalesOrders_Success() {
        // Given
        when(salesOrderRepository.findAll()).thenReturn(Arrays.asList(salesOrder));

        // When
        ApiResponse<List<SalesOrderResponseDto>> response =
                salesOrderService.getAllSalesOrders();

        // Then
        assertNotNull(response);
        assertEquals("Sales orders retrieved successfully", response.getMessage());
        assertEquals(1, response.getData().size());
        verify(salesOrderRepository).findAll();
    }

    @Test
    void getSalesOrdersByStatus_Success() {
        // Given
        when(salesOrderRepository.findByStatus(OrderStatus.CREATED))
                .thenReturn(Arrays.asList(salesOrder));

        // When
        ApiResponse<List<SalesOrderResponseDto>> response =
                salesOrderService.getSalesOrdersByStatus(OrderStatus.CREATED);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getData().size());
        verify(salesOrderRepository).findByStatus(OrderStatus.CREATED);
    }

    @Test
    void getSalesOrdersByClient_Success() {
        // Given
        when(clientRepository.existsById(1L)).thenReturn(true);
        when(salesOrderRepository.findByClientId(1L)).thenReturn(Arrays.asList(salesOrder));

        // When
        ApiResponse<List<SalesOrderResponseDto>> response =
                salesOrderService.getSalesOrdersByClient(1L);

        // Then
        assertNotNull(response);
        assertEquals("Client sales orders retrieved successfully", response.getMessage());
        assertEquals(1, response.getData().size());
        verify(clientRepository).existsById(1L);
        verify(salesOrderRepository).findByClientId(1L);
    }

    @Test
    void getSalesOrdersByClient_ClientNotFound_ThrowsException() {
        // Given
        when(clientRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> salesOrderService.getSalesOrdersByClient(999L));

        verify(clientRepository).existsById(999L);
        verify(salesOrderRepository, never()).findByClientId(anyLong());
    }

    @Test
    void countSalesOrders_Success() {
        // Given
        when(salesOrderRepository.count()).thenReturn(15L);

        // When
        ApiResponse<Long> response = salesOrderService.countSalesOrders();

        // Then
        assertNotNull(response);
        assertEquals(15L, response.getData());
        verify(salesOrderRepository).count();
    }
}