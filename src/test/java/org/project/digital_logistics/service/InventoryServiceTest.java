package org.project.digital_logistics.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.inventory.InventoryRequestDto;
import org.project.digital_logistics.dto.inventory.InventoryResponseDto;
import org.project.digital_logistics.exception.DuplicateResourceException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.model.Inventory;
import org.project.digital_logistics.model.Product;
import org.project.digital_logistics.model.Warehouse;
import org.project.digital_logistics.model.enums.MovementType;
import org.project.digital_logistics.repository.InventoryRepository;
import org.project.digital_logistics.repository.ProductRepository;
import org.project.digital_logistics.repository.WarehouseRepository;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryMovementService movementService;

    @InjectMocks
    private InventoryService inventoryService;

    private Inventory inventory;
    private Warehouse warehouse;
    private Product product;
    private InventoryRequestDto requestDto;

    @BeforeEach
    void setUp() {
        // Setup Warehouse
        warehouse = Warehouse.builder()
                .id(1L)
                .code("WH-001")
                .name("Main Warehouse")
                .capacity(10000)
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

        // Setup Inventory
        inventory = Inventory.builder()
                .id(1L)
                .warehouse(warehouse)
                .product(product)
                .qtyOnHand(100)
                .qtyReserved(10)
                .build();

        // Setup Request DTO
        requestDto = InventoryRequestDto.builder()
                .warehouseId(1L)
                .productId(1L)
                .qtyOnHand(100)
                .qtyReserved(10)
                .build();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CREATE INVENTORY TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void createInventory_Success() {
        // Given
        when(inventoryRepository.existsByWarehouseIdAndProductId(1L, 1L)).thenReturn(false);
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        // When
        ApiResponse<InventoryResponseDto> response = inventoryService.createInventory(requestDto);

        // Then
        assertNotNull(response);
        assertEquals("Inventory created successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(1L, response.getData().getId());

        verify(inventoryRepository).existsByWarehouseIdAndProductId(1L, 1L);
        verify(warehouseRepository).findById(1L);
        verify(productRepository).findById(1L);
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void createInventory_DuplicateInventory_ThrowsException() {
        // Given
        when(inventoryRepository.existsByWarehouseIdAndProductId(1L, 1L)).thenReturn(true);

        // When & Then
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> inventoryService.createInventory(requestDto)
        );

        assertTrue(exception.getMessage().contains("already exists"));
        verify(inventoryRepository).existsByWarehouseIdAndProductId(1L, 1L);
        verify(warehouseRepository, never()).findById(anyLong());
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void createInventory_WarehouseNotFound_ThrowsException() {
        // Given
        when(inventoryRepository.existsByWarehouseIdAndProductId(1L, 1L)).thenReturn(false);
        when(warehouseRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> inventoryService.createInventory(requestDto));

        verify(warehouseRepository).findById(1L);
        verify(productRepository, never()).findById(anyLong());
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void createInventory_InvalidQuantities_ThrowsException() {
        // Given
        requestDto.setQtyReserved(150); // Greater than qtyOnHand (100)
        when(inventoryRepository.existsByWarehouseIdAndProductId(1L, 1L)).thenReturn(false);
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> inventoryService.createInventory(requestDto)
        );

        assertTrue(exception.getMessage().contains("cannot exceed"));
        verify(inventoryRepository, never()).save(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // GET INVENTORY TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getInventoryById_Success() {
        // Given
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));

        // When
        ApiResponse<InventoryResponseDto> response = inventoryService.getInventoryById(1L);

        // Then
        assertNotNull(response);
        assertEquals("Inventory retrieved successfully", response.getMessage());
        assertEquals(1L, response.getData().getId());
        verify(inventoryRepository).findById(1L);
    }

    @Test
    void getInventoryById_NotFound_ThrowsException() {
        // Given
        when(inventoryRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> inventoryService.getInventoryById(999L));

        verify(inventoryRepository).findById(999L);
    }

    @Test
    void getInventoryByWarehouseAndProduct_Success() {
        // Given
        when(inventoryRepository.findByWarehouseIdAndProductId(1L, 1L))
                .thenReturn(Optional.of(inventory));

        // When
        ApiResponse<InventoryResponseDto> response =
                inventoryService.getInventoryByWarehouseAndProduct(1L, 1L);

        // Then
        assertNotNull(response);
        assertEquals("Inventory retrieved successfully", response.getMessage());
        verify(inventoryRepository).findByWarehouseIdAndProductId(1L, 1L);
    }

    @Test
    void getInventoryByWarehouseAndProduct_NotFound_ThrowsException() {
        // Given
        when(inventoryRepository.findByWarehouseIdAndProductId(999L, 999L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> inventoryService.getInventoryByWarehouseAndProduct(999L, 999L));

        verify(inventoryRepository).findByWarehouseIdAndProductId(999L, 999L);
    }

    @Test
    void getAllInventories_Success() {
        // Given
        when(inventoryRepository.findAll()).thenReturn(Arrays.asList(inventory));

        // When
        ApiResponse<List<InventoryResponseDto>> response = inventoryService.getAllInventories();

        // Then
        assertNotNull(response);
        assertEquals("Inventories retrieved successfully", response.getMessage());
        assertEquals(1, response.getData().size());
        verify(inventoryRepository).findAll();
    }

    @Test
    void getInventoriesByWarehouse_Success() {
        // Given
        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(inventoryRepository.findByWarehouseId(1L)).thenReturn(Arrays.asList(inventory));

        // When
        ApiResponse<List<InventoryResponseDto>> response =
                inventoryService.getInventoriesByWarehouse(1L);

        // Then
        assertNotNull(response);
        assertEquals("Warehouse inventories retrieved successfully", response.getMessage());
        assertEquals(1, response.getData().size());
        verify(warehouseRepository).existsById(1L);
        verify(inventoryRepository).findByWarehouseId(1L);
    }

    @Test
    void getInventoriesByWarehouse_WarehouseNotFound_ThrowsException() {
        // Given
        when(warehouseRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> inventoryService.getInventoriesByWarehouse(999L));

        verify(warehouseRepository).existsById(999L);
        verify(inventoryRepository, never()).findByWarehouseId(anyLong());
    }

    @Test
    void getInventoriesByProduct_Success() {
        // Given
        when(productRepository.existsById(1L)).thenReturn(true);
        when(inventoryRepository.findByProductId(1L)).thenReturn(Arrays.asList(inventory));

        // When
        ApiResponse<List<InventoryResponseDto>> response =
                inventoryService.getInventoriesByProduct(1L);

        // Then
        assertNotNull(response);
        assertEquals("Product inventories retrieved successfully", response.getMessage());
        assertEquals(1, response.getData().size());
        verify(productRepository).existsById(1L);
        verify(inventoryRepository).findByProductId(1L);
    }

    @Test
    void getInventoriesByProduct_ProductNotFound_ThrowsException() {
        // Given
        when(productRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> inventoryService.getInventoriesByProduct(999L));

        verify(productRepository).existsById(999L);
        verify(inventoryRepository, never()).findByProductId(anyLong());
    }

    @Test
    void getLowStockInWarehouse_Success() {
        // Given
        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(inventoryRepository.findLowStockInWarehouse(1L, 20))
                .thenReturn(Arrays.asList(inventory));

        // When
        ApiResponse<List<InventoryResponseDto>> response =
                inventoryService.getLowStockInWarehouse(1L, 20);

        // Then
        assertNotNull(response);
        assertEquals("Low stock items retrieved successfully", response.getMessage());
        assertEquals(1, response.getData().size());
        verify(inventoryRepository).findLowStockInWarehouse(1L, 20);
    }

    @Test
    void getLowStockInWarehouse_DefaultThreshold_Success() {
        // Given
        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(inventoryRepository.findLowStockInWarehouse(1L, 10))
                .thenReturn(Arrays.asList(inventory));

        // When
        ApiResponse<List<InventoryResponseDto>> response =
                inventoryService.getLowStockInWarehouse(1L, null);

        // Then
        assertNotNull(response);
        verify(inventoryRepository).findLowStockInWarehouse(1L, 10); // Default threshold
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // UPDATE INVENTORY TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void updateInventory_Success() {
        // Given
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        // When
        ApiResponse<InventoryResponseDto> response =
                inventoryService.updateInventory(1L, requestDto);

        // Then
        assertNotNull(response);
        assertEquals("Inventory updated successfully", response.getMessage());
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    void updateInventory_NotFound_ThrowsException() {
        // Given
        when(inventoryRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> inventoryService.updateInventory(999L, requestDto));

        verify(inventoryRepository).findById(999L);
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void updateInventory_ChangingWarehouse_DuplicateCheck() {
        // Given
        requestDto.setWarehouseId(2L); // Different warehouse
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.existsByWarehouseIdAndProductId(2L, 1L)).thenReturn(true);

        // When & Then
        assertThrows(DuplicateResourceException.class,
                () -> inventoryService.updateInventory(1L, requestDto));

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void updateInventory_InvalidQuantities_ThrowsException() {
        // Given
        requestDto.setQtyReserved(200); // Greater than qtyOnHand
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> inventoryService.updateInventory(1L, requestDto));

        verify(inventoryRepository, never()).save(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // ADJUST QUANTITIES TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void adjustQuantities_BothValues_Success() {
        // Given
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        doNothing().when(movementService).recordMovement(
                anyLong(), any(MovementType.class), anyInt(), anyString(), anyString());

        // When
        ApiResponse<InventoryResponseDto> response =
                inventoryService.adjustQuantities(1L, 150, 20);

        // Then
        assertNotNull(response);
        assertEquals("Inventory quantities adjusted successfully", response.getMessage());
        verify(inventoryRepository).save(any(Inventory.class));
        verify(movementService).recordMovement(
                anyLong(), eq(MovementType.ADJUSTMENT), anyInt(), anyString(), anyString());
    }

    @Test
    void adjustQuantities_OnlyQtyOnHand_Success() {
        // Given
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        doNothing().when(movementService).recordMovement(
                anyLong(), any(MovementType.class), anyInt(), anyString(), anyString());

        // When
        ApiResponse<InventoryResponseDto> response =
                inventoryService.adjustQuantities(1L, 120, null);

        // Then
        assertNotNull(response);
        verify(inventoryRepository).save(any(Inventory.class));
        verify(movementService).recordMovement(anyLong(), any(), anyInt(), anyString(), anyString());
    }

    @Test
    void adjustQuantities_InvalidQuantities_ThrowsException() {
        // Given
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> inventoryService.adjustQuantities(1L, 50, 100)); // Reserved > OnHand

        verify(inventoryRepository, never()).save(any());
        verify(movementService, never()).recordMovement(anyLong(), any(), anyInt(), anyString(), anyString());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // DELETE INVENTORY TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void deleteInventory_Success() {
        // Given
        when(inventoryRepository.existsById(1L)).thenReturn(true);
        doNothing().when(inventoryRepository).deleteById(1L);

        // When
        ApiResponse<Void> response = inventoryService.deleteInventory(1L);

        // Then
        assertNotNull(response);
        assertEquals("Inventory deleted successfully", response.getMessage());
        verify(inventoryRepository).deleteById(1L);
    }

    @Test
    void deleteInventory_NotFound_ThrowsException() {
        // Given
        when(inventoryRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> inventoryService.deleteInventory(999L));

        verify(inventoryRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteInventoryByWarehouseAndProduct_Success() {
        // Given
        when(inventoryRepository.existsByWarehouseIdAndProductId(1L, 1L)).thenReturn(true);
        doNothing().when(inventoryRepository).deleteByWarehouseIdAndProductId(1L, 1L);

        // When
        ApiResponse<Void> response =
                inventoryService.deleteInventoryByWarehouseAndProduct(1L, 1L);

        // Then
        assertNotNull(response);
        assertEquals("Inventory deleted successfully", response.getMessage());
        verify(inventoryRepository).deleteByWarehouseIdAndProductId(1L, 1L);
    }

    @Test
    void deleteInventoryByWarehouseAndProduct_NotFound_ThrowsException() {
        // Given
        when(inventoryRepository.existsByWarehouseIdAndProductId(999L, 999L)).thenReturn(false);

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> inventoryService.deleteInventoryByWarehouseAndProduct(999L, 999L));

        verify(inventoryRepository, never()).deleteByWarehouseIdAndProductId(anyLong(), anyLong());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // STOCK QUERIES TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getTotalStockByProduct_Success() {
        // Given
        when(productRepository.existsById(1L)).thenReturn(true);
        when(inventoryRepository.getTotalStockByProduct(1L)).thenReturn(500);

        // When
        ApiResponse<Integer> response = inventoryService.getTotalStockByProduct(1L);

        // Then
        assertNotNull(response);
        assertEquals("Total stock retrieved successfully", response.getMessage());
        assertEquals(500, response.getData());
        verify(inventoryRepository).getTotalStockByProduct(1L);
    }

    @Test
    void getTotalStockByProduct_ProductNotFound_ThrowsException() {
        // Given
        when(productRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> inventoryService.getTotalStockByProduct(999L));

        verify(inventoryRepository, never()).getTotalStockByProduct(anyLong());
    }

    @Test
    void getAvailableStockByProduct_Success() {
        // Given
        when(productRepository.existsById(1L)).thenReturn(true);
        when(inventoryRepository.getAvailableStockByProduct(1L)).thenReturn(450);

        // When
        ApiResponse<Integer> response = inventoryService.getAvailableStockByProduct(1L);

        // Then
        assertNotNull(response);
        assertEquals("Available stock retrieved successfully", response.getMessage());
        assertEquals(450, response.getData());
        verify(inventoryRepository).getAvailableStockByProduct(1L);
    }

    @Test
    void getAvailableStockByProduct_ProductNotFound_ThrowsException() {
        // Given
        when(productRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> inventoryService.getAvailableStockByProduct(999L));

        verify(inventoryRepository, never()).getAvailableStockByProduct(anyLong());
    }

    @Test
    void countInventories_Success() {
        // Given
        when(inventoryRepository.count()).thenReturn(75L);

        // When
        ApiResponse<Long> response = inventoryService.countInventories();

        // Then
        assertNotNull(response);
        assertEquals("Total inventories counted successfully", response.getMessage());
        assertEquals(75L, response.getData());
        verify(inventoryRepository).count();
    }
}