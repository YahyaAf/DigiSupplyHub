package org.project.digital_logistics.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.inventorymovement.InventoryMovementRequestDto;
import org.project.digital_logistics.dto.inventorymovement.InventoryMovementResponseDto;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.model.Inventory;
import org.project.digital_logistics.model.InventoryMovement;
import org.project.digital_logistics.model.Product;
import org.project.digital_logistics.model.Warehouse;
import org.project.digital_logistics.model.enums.MovementType;
import org.project.digital_logistics.repository.InventoryMovementRepository;
import org.project.digital_logistics.repository.InventoryRepository;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryMovementServiceTest {

    @Mock
    private InventoryMovementRepository movementRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryMovementService movementService;

    private Inventory inventory;
    private Warehouse warehouse;
    private Product product;
    private InventoryMovement movement;
    private InventoryMovementRequestDto requestDto;

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

        // Setup InventoryMovement
        movement = InventoryMovement.builder()
                .id(1L)
                .inventory(inventory)
                .type(MovementType.INBOUND)
                .quantity(50)
                .referenceDocument("PO-123")
                .description("Purchase order reception")
                .build();

        // Setup Request DTO
        requestDto = InventoryMovementRequestDto.builder()
                .inventoryId(1L)
                .type(MovementType.INBOUND)
                .quantity(50)
                .referenceDocument("PO-123")
                .description("Purchase order reception")
                .build();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CREATE MOVEMENT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void createMovement_Success() {
        // Given
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(movementRepository.save(any(InventoryMovement.class))).thenReturn(movement);

        // When
        ApiResponse<InventoryMovementResponseDto> response =
                movementService.createMovement(requestDto);

        // Then
        assertNotNull(response);
        assertEquals("Inventory movement recorded successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(1L, response.getData().getId());
        assertEquals(MovementType.INBOUND, response.getData().getType());
        assertEquals(50, response.getData().getQuantity());

        verify(inventoryRepository).findById(1L);
        verify(movementRepository).save(any(InventoryMovement.class));
    }

    @Test
    void createMovement_InventoryNotFound_ThrowsException() {
        // Given
        when(inventoryRepository.findById(999L)).thenReturn(Optional.empty());
        requestDto.setInventoryId(999L);

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> movementService.createMovement(requestDto)
        );

        assertTrue(exception.getMessage().contains("Inventory"));
        assertTrue(exception.getMessage().contains("id"));

        verify(inventoryRepository).findById(999L);
        verify(movementRepository, never()).save(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // RECORD MOVEMENT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void recordMovement_Success() {
        // Given
        when(inventoryRepository.findById(1L)).thenReturn(Optional.of(inventory));
        when(movementRepository.save(any(InventoryMovement.class))).thenReturn(movement);

        // When
        movementService.recordMovement(
                1L,
                MovementType.OUTBOUND,
                20,
                "SO-456",
                "Sales order shipment"
        );

        // Then
        ArgumentCaptor<InventoryMovement> captor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(movementRepository).save(captor.capture());

        InventoryMovement savedMovement = captor.getValue();
        assertEquals(inventory, savedMovement.getInventory());
        assertEquals(MovementType.OUTBOUND, savedMovement.getType());
        assertEquals(20, savedMovement.getQuantity());
        assertEquals("SO-456", savedMovement.getReferenceDocument());
        assertEquals("Sales order shipment", savedMovement.getDescription());

        verify(inventoryRepository).findById(1L);
    }

    @Test
    void recordMovement_InventoryNotFound_ThrowsException() {
        // Given
        when(inventoryRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> movementService.recordMovement(
                        999L,
                        MovementType.ADJUSTMENT,
                        10,
                        "ADJ-001",
                        "Adjustment"
                )
        );

        assertTrue(exception.getMessage().contains("Inventory"));

        verify(inventoryRepository).findById(999L);
        verify(movementRepository, never()).save(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // GET ALL MOVEMENTS TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getAllMovements_Success() {
        // Given
        InventoryMovement movement2 = InventoryMovement.builder()
                .id(2L)
                .inventory(inventory)
                .type(MovementType.OUTBOUND)
                .quantity(30)
                .referenceDocument("SO-789")
                .description("Sales order")
                .build();

        List<InventoryMovement> movements = Arrays.asList(movement, movement2);
        when(movementRepository.findAll()).thenReturn(movements);

        // When
        ApiResponse<List<InventoryMovementResponseDto>> response =
                movementService.getAllMovements();

        // Then
        assertNotNull(response);
        assertEquals("Inventory movements retrieved successfully", response.getMessage());
        assertEquals(2, response.getData().size());
        assertEquals(1L, response.getData().get(0).getId());
        assertEquals(2L, response.getData().get(1).getId());

        verify(movementRepository).findAll();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // GET MOVEMENTS BY INVENTORY TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getMovementsByInventory_Success() {
        // Given
        when(inventoryRepository.existsById(1L)).thenReturn(true);
        when(movementRepository.findByInventoryId(1L)).thenReturn(Arrays.asList(movement));

        // When
        ApiResponse<List<InventoryMovementResponseDto>> response =
                movementService.getMovementsByInventory(1L);

        // Then
        assertNotNull(response);
        assertEquals("Inventory movements retrieved successfully", response.getMessage());
        assertEquals(1, response.getData().size());
        assertEquals(1L, response.getData().get(0).getId());

        verify(inventoryRepository).existsById(1L);
        verify(movementRepository).findByInventoryId(1L);
    }

    @Test
    void getMovementsByInventory_InventoryNotFound_ThrowsException() {
        // Given
        when(inventoryRepository.existsById(999L)).thenReturn(false);

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> movementService.getMovementsByInventory(999L)
        );

        assertTrue(exception.getMessage().contains("Inventory"));

        verify(inventoryRepository).existsById(999L);
        verify(movementRepository, never()).findByInventoryId(anyLong());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // GET MOVEMENTS BY WAREHOUSE TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getMovementsByWarehouse_Success() {
        // Given
        when(movementRepository.findByWarehouseId(1L)).thenReturn(Arrays.asList(movement));

        // When
        ApiResponse<List<InventoryMovementResponseDto>> response =
                movementService.getMovementsByWarehouse(1L);

        // Then
        assertNotNull(response);
        assertEquals("Warehouse movements retrieved successfully", response.getMessage());
        assertEquals(1, response.getData().size());
        assertEquals(1L, response.getData().get(0).getId());

        verify(movementRepository).findByWarehouseId(1L);
    }
}