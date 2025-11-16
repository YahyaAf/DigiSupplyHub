package org.project.digital_logistics.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.purchaseorder.PurchaseOrderLineDto;
import org.project.digital_logistics.dto.purchaseorder.PurchaseOrderRequestDto;
import org.project.digital_logistics.dto.purchaseorder.PurchaseOrderResponseDto;
import org.project.digital_logistics.enums.PurchaseOrderStatus;
import org.project.digital_logistics.exception.InvalidOperationException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.model.*;
import org.project.digital_logistics.model.enums.MovementType;
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
class PurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private InventoryMovementService movementService;

    @InjectMocks
    private PurchaseOrderService purchaseOrderService;

    private Supplier supplier;
    private Product product;
    private Warehouse warehouse;
    private User manager;
    private PurchaseOrder purchaseOrder;
    private PurchaseOrderRequestDto requestDto;
    private PurchaseOrderLineDto lineDto;

    @BeforeEach
    void setUp() {
        // ✅ Setup Manager (User)
        manager = User.builder()
                .id(1L)
                .name("manager")
                .email("manager@test.com")
                .active(true)
                .build();

        // ✅ Setup Supplier (Match SupplierRequestDto fields)
        supplier = Supplier.builder()
                .id(1L)
                .name("Test Supplier")              // ✅ name (not contactName)
                .phoneNumber("0612345678")          // ✅ phoneNumber (not phone)
                .address("123 Test Street, Casa")   // ✅ address (required)
                .matricule("SUP-001")               // ✅ matricule (required)
                .build();

        // ✅ Setup Product
        product = Product.builder()
                .id(1L)
                .sku("PROD-001")
                .name("Test Product")
                .category("Electronics")
                .active(true)
                .originalPrice(10000L)
                .profite(BigDecimal.valueOf(1000))
                .build();

        // ✅ Setup Warehouse (Match WarehouseRequestDto fields)
        warehouse = Warehouse.builder()
                .id(1L)
                .code("WH-001")                     // ✅ code (required)
                .name("Main Warehouse")             // ✅ name (required)
                .capacity(10000)                    // ✅ capacity (required)
                .manager(manager)                   // ✅ manager (required)
                .active(true)
                .build();

        // ✅ Setup PurchaseOrderLineDto (unitPrice as BigDecimal)
        lineDto = PurchaseOrderLineDto.builder()
                .productId(1L)
                .quantity(10)
                .unitPrice(BigDecimal.valueOf(100))  // ✅ BigDecimal (not Long)
                .build();

        // ✅ Setup PurchaseOrderRequestDto
        requestDto = PurchaseOrderRequestDto.builder()
                .supplierId(1L)
                .expectedDelivery(LocalDateTime.now().plusDays(7))
                .orderLines(Arrays.asList(lineDto))
                .build();

        // ✅ Setup PurchaseOrder
        purchaseOrder = PurchaseOrder.builder()
                .id(1L)
                .supplier(supplier)
                .status(PurchaseOrderStatus.CREATED)
                .expectedDelivery(LocalDateTime.now().plusDays(7))
                .build();

        // ✅ Setup PurchaseOrderLine (unitPrice as Long in entity)
        PurchaseOrderLine line = PurchaseOrderLine.builder()
                .id(1L)
                .purchaseOrder(purchaseOrder)
                .product(product)
                .quantity(10)
                .unitPrice(BigDecimal.valueOf(100))  // ✅ Entity uses Long, DTO uses BigDecimal
                .build();

        purchaseOrder.addOrderLine(line);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CREATE PURCHASE ORDER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void createPurchaseOrder_Success() {
        // Given
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(purchaseOrder);

        // When
        ApiResponse<PurchaseOrderResponseDto> response = purchaseOrderService.createPurchaseOrder(requestDto);

        // Then
        assertNotNull(response);
        assertEquals("Purchase order created successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(1L, response.getData().getId());

        verify(supplierRepository).findById(1L);
        verify(productRepository).findById(1L);
        verify(purchaseOrderRepository).save(any(PurchaseOrder.class));
    }

    @Test
    void createPurchaseOrder_SupplierNotFound_ThrowsException() {
        // Given
        when(supplierRepository.findById(999L)).thenReturn(Optional.empty());
        requestDto.setSupplierId(999L);

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> purchaseOrderService.createPurchaseOrder(requestDto));

        verify(supplierRepository).findById(999L);
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    void createPurchaseOrder_ProductNotFound_ThrowsException() {
        // Given
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());
        lineDto.setProductId(999L);

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> purchaseOrderService.createPurchaseOrder(requestDto));

        verify(supplierRepository).findById(1L);
        verify(productRepository).findById(999L);
        verify(purchaseOrderRepository, never()).save(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // GET PURCHASE ORDER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getPurchaseOrderById_Success() {
        // Given
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));

        // When
        ApiResponse<PurchaseOrderResponseDto> response = purchaseOrderService.getPurchaseOrderById(1L);

        // Then
        assertNotNull(response);
        assertEquals("Purchase order retrieved successfully", response.getMessage());
        assertEquals(1L, response.getData().getId());
        verify(purchaseOrderRepository).findById(1L);
    }

    @Test
    void getPurchaseOrderById_NotFound_ThrowsException() {
        // Given
        when(purchaseOrderRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> purchaseOrderService.getPurchaseOrderById(999L));

        verify(purchaseOrderRepository).findById(999L);
    }

    @Test
    void getAllPurchaseOrders_Success() {
        // Given
        List<PurchaseOrder> orders = Arrays.asList(purchaseOrder);
        when(purchaseOrderRepository.findAll()).thenReturn(orders);

        // When
        ApiResponse<List<PurchaseOrderResponseDto>> response = purchaseOrderService.getAllPurchaseOrders();

        // Then
        assertNotNull(response);
        assertEquals("Purchase orders retrieved successfully", response.getMessage());
        assertEquals(1, response.getData().size());
        verify(purchaseOrderRepository).findAll();
    }

    @Test
    void getPurchaseOrdersByStatus_Success() {
        // Given
        List<PurchaseOrder> orders = Arrays.asList(purchaseOrder);
        when(purchaseOrderRepository.findByStatus(PurchaseOrderStatus.CREATED)).thenReturn(orders);

        // When
        ApiResponse<List<PurchaseOrderResponseDto>> response =
                purchaseOrderService.getPurchaseOrdersByStatus(PurchaseOrderStatus.CREATED);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getData().size());
        verify(purchaseOrderRepository).findByStatus(PurchaseOrderStatus.CREATED);
    }

    @Test
    void getPurchaseOrdersBySupplier_Success() {
        // Given
        when(supplierRepository.existsById(1L)).thenReturn(true);
        when(purchaseOrderRepository.findBySupplierId(1L)).thenReturn(Arrays.asList(purchaseOrder));

        // When
        ApiResponse<List<PurchaseOrderResponseDto>> response =
                purchaseOrderService.getPurchaseOrdersBySupplier(1L);

        // Then
        assertNotNull(response);
        assertEquals("Supplier purchase orders retrieved successfully", response.getMessage());
        assertEquals(1, response.getData().size());
        verify(supplierRepository).existsById(1L);
        verify(purchaseOrderRepository).findBySupplierId(1L);
    }

    @Test
    void getPurchaseOrdersBySupplier_SupplierNotFound_ThrowsException() {
        // Given
        when(supplierRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> purchaseOrderService.getPurchaseOrdersBySupplier(999L));

        verify(supplierRepository).existsById(999L);
        verify(purchaseOrderRepository, never()).findBySupplierId(anyLong());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // UPDATE PURCHASE ORDER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void updatePurchaseOrder_Success() {
        // Given
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(purchaseOrder);

        // When
        ApiResponse<PurchaseOrderResponseDto> response =
                purchaseOrderService.updatePurchaseOrder(1L, requestDto);

        // Then
        assertNotNull(response);
        assertEquals("Purchase order updated successfully", response.getMessage());
        verify(purchaseOrderRepository).save(any(PurchaseOrder.class));
    }

    @Test
    void updatePurchaseOrder_NotFound_ThrowsException() {
        // Given
        when(purchaseOrderRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> purchaseOrderService.updatePurchaseOrder(999L, requestDto));

        verify(purchaseOrderRepository).findById(999L);
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    void updatePurchaseOrder_ReceivedStatus_ThrowsException() {
        // Given
        purchaseOrder.setStatus(PurchaseOrderStatus.RECEIVED);
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));

        // When & Then
        assertThrows(InvalidOperationException.class,
                () -> purchaseOrderService.updatePurchaseOrder(1L, requestDto));

        verify(purchaseOrderRepository).findById(1L);
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    void updatePurchaseOrder_CanceledStatus_ThrowsException() {
        // Given
        purchaseOrder.setStatus(PurchaseOrderStatus.CANCELED);
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));

        // When & Then
        assertThrows(InvalidOperationException.class,
                () -> purchaseOrderService.updatePurchaseOrder(1L, requestDto));

        verify(purchaseOrderRepository, never()).save(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // APPROVE PURCHASE ORDER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void approvePurchaseOrder_Success() {
        // Given
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(purchaseOrder);

        // When
        ApiResponse<PurchaseOrderResponseDto> response =
                purchaseOrderService.approvePurchaseOrder(1L);

        // Then
        assertNotNull(response);
        assertEquals("Purchase order approved successfully", response.getMessage());

        ArgumentCaptor<PurchaseOrder> captor = ArgumentCaptor.forClass(PurchaseOrder.class);
        verify(purchaseOrderRepository).save(captor.capture());

        PurchaseOrder savedOrder = captor.getValue();
        assertEquals(PurchaseOrderStatus.APPROVED, savedOrder.getStatus());
        assertNotNull(savedOrder.getApprovedAt());
    }

    @Test
    void approvePurchaseOrder_NotCreatedStatus_ThrowsException() {
        // Given
        purchaseOrder.setStatus(PurchaseOrderStatus.APPROVED);
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));

        // When & Then
        assertThrows(InvalidOperationException.class,
                () -> purchaseOrderService.approvePurchaseOrder(1L));

        verify(purchaseOrderRepository, never()).save(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // RECEIVE PURCHASE ORDER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void receivePurchaseOrder_Success() {
        // Given
        purchaseOrder.setStatus(PurchaseOrderStatus.APPROVED);

        Inventory inventory = Inventory.builder()
                .id(1L)
                .warehouse(warehouse)
                .product(product)
                .qtyOnHand(0)
                .qtyReserved(0)
                .build();

        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(inventoryRepository.findByWarehouseIdAndProductId(1L, 1L))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(purchaseOrder);
        doNothing().when(movementService).recordMovement(anyLong(), any(), anyInt(), anyString(), anyString());

        // When
        ApiResponse<PurchaseOrderResponseDto> response =
                purchaseOrderService.receivePurchaseOrder(1L, 1L);

        // Then
        assertNotNull(response);
        assertEquals("Purchase order received successfully and inventory updated", response.getMessage());

        verify(inventoryRepository).save(any(Inventory.class));
        verify(movementService).recordMovement(
                eq(1L),
                eq(MovementType.INBOUND),
                eq(10),
                eq("PO-1"),
                anyString()
        );

        ArgumentCaptor<PurchaseOrder> captor = ArgumentCaptor.forClass(PurchaseOrder.class);
        verify(purchaseOrderRepository).save(captor.capture());

        PurchaseOrder savedOrder = captor.getValue();
        assertEquals(PurchaseOrderStatus.RECEIVED, savedOrder.getStatus());
        assertNotNull(savedOrder.getReceivedAt());
    }

    @Test
    void receivePurchaseOrder_NotApprovedStatus_ThrowsException() {
        // Given
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));

        // When & Then
        assertThrows(InvalidOperationException.class,
                () -> purchaseOrderService.receivePurchaseOrder(1L, 1L));

        verify(warehouseRepository, never()).findById(anyLong());
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void receivePurchaseOrder_WarehouseNotFound_ThrowsException() {
        // Given
        purchaseOrder.setStatus(PurchaseOrderStatus.APPROVED);
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));
        when(warehouseRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> purchaseOrderService.receivePurchaseOrder(1L, 999L));

        verify(inventoryRepository, never()).save(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CANCEL & DELETE TESTS (Same as before)
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void cancelPurchaseOrder_Success() {
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenReturn(purchaseOrder);

        ApiResponse<PurchaseOrderResponseDto> response = purchaseOrderService.cancelPurchaseOrder(1L);

        assertNotNull(response);
        assertEquals("Purchase order canceled successfully", response.getMessage());
        verify(purchaseOrderRepository).save(any(PurchaseOrder.class));
    }

    @Test
    void deletePurchaseOrder_CreatedStatus_Success() {
        when(purchaseOrderRepository.findById(1L)).thenReturn(Optional.of(purchaseOrder));
        doNothing().when(purchaseOrderRepository).deleteById(1L);

        ApiResponse<Void> response = purchaseOrderService.deletePurchaseOrder(1L);

        assertNotNull(response);
        verify(purchaseOrderRepository).deleteById(1L);
    }

    @Test
    void countPurchaseOrders_Success() {
        when(purchaseOrderRepository.count()).thenReturn(10L);

        ApiResponse<Long> response = purchaseOrderService.countPurchaseOrders();

        assertEquals(10L, response.getData());
        verify(purchaseOrderRepository).count();
    }

    @Test
    void countPurchaseOrdersByStatus_Success() {
        when(purchaseOrderRepository.countByStatus(PurchaseOrderStatus.CREATED)).thenReturn(5L);

        ApiResponse<Long> response =
                purchaseOrderService.countPurchaseOrdersByStatus(PurchaseOrderStatus.CREATED);

        assertEquals(5L, response.getData());
        verify(purchaseOrderRepository).countByStatus(PurchaseOrderStatus.CREATED);
    }
}