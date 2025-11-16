package org.project.digital_logistics.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.warehouse.WarehouseRequestDto;
import org.project.digital_logistics.dto.warehouse.WarehouseResponseDto;
import org.project.digital_logistics.exception.DuplicateResourceException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.model.User;
import org.project.digital_logistics.model.Warehouse;
import org.project.digital_logistics.model.enums.Role;
import org.project.digital_logistics.repository.UserRepository;
import org.project.digital_logistics.repository.WarehouseRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WarehouseService warehouseService;

    private Warehouse warehouse;
    private User manager;
    private WarehouseRequestDto requestDto;

    @BeforeEach
    void setUp() {
        // Setup Manager (User)
        manager = User.builder()
                .id(1L)
                .name("warehouse_manager")
                .email("manager@example.com")
                .passwordHash("encodedPassword")
                .role(Role.WAREHOUSE_MANAGER)
                .active(true)
                .build();

        // Setup WarehouseRequestDto
        requestDto = new WarehouseRequestDto();
        requestDto.setName("Main Warehouse");
        requestDto.setCode("WH-001");
        requestDto.setCapacity(10000);
        requestDto.setActive(true);
        requestDto.setManagerId(1L);

        // Setup Warehouse Entity
        warehouse = Warehouse.builder()
                .id(1L)
                .name("Main Warehouse")
                .code("WH-001")
                .capacity(10000)
                .manager(manager)
                .active(true)
                .build();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CREATE WAREHOUSE TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void createWarehouse_Success() {
        // Given
        when(warehouseRepository.existsByCode("WH-001")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(manager));
        when(warehouseRepository.save(any(Warehouse.class))).thenReturn(warehouse);

        // When
        ApiResponse<WarehouseResponseDto> response = warehouseService.createWarehouse(requestDto);

        // Then
        assertNotNull(response);
        assertEquals("Warehouse created successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals("WH-001", response.getData().getCode());
        assertEquals("Main Warehouse", response.getData().getName());

        verify(warehouseRepository).existsByCode("WH-001");
        verify(userRepository).findById(1L);
        verify(warehouseRepository).save(any(Warehouse.class));
    }

    @Test
    void createWarehouse_DuplicateCode_ThrowsException() {
        // Given
        when(warehouseRepository.existsByCode("WH-001")).thenReturn(true);

        // When & Then
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> warehouseService.createWarehouse(requestDto)
        );

        assertTrue(exception.getMessage().contains("code"));
        assertTrue(exception.getMessage().contains("WH-001"));

        verify(warehouseRepository).existsByCode("WH-001");
        verify(userRepository, never()).findById(anyLong());
        verify(warehouseRepository, never()).save(any());
    }

    @Test
    void createWarehouse_ManagerNotFound_ThrowsException() {
        // Given
        when(warehouseRepository.existsByCode("WH-001")).thenReturn(false);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        requestDto.setManagerId(999L);

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> warehouseService.createWarehouse(requestDto)
        );

        assertTrue(exception.getMessage().contains("User"));
        assertTrue(exception.getMessage().contains("id"));

        verify(warehouseRepository).existsByCode("WH-001");
        verify(userRepository).findById(999L);
        verify(warehouseRepository, never()).save(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // GET WAREHOUSE TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getWarehouseById_Success() {
        // Given
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));

        // When
        ApiResponse<WarehouseResponseDto> response = warehouseService.getWarehouseById(1L);

        // Then
        assertNotNull(response);
        assertEquals("Warehouse retrieved successfully", response.getMessage());
        assertEquals(1L, response.getData().getId());
        assertEquals("WH-001", response.getData().getCode());

        verify(warehouseRepository).findById(1L);
    }

    @Test
    void getWarehouseById_NotFound_ThrowsException() {
        // Given
        when(warehouseRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> warehouseService.getWarehouseById(999L)
        );

        assertTrue(exception.getMessage().contains("Warehouse"));
        assertTrue(exception.getMessage().contains("id"));

        verify(warehouseRepository).findById(999L);
    }

    @Test
    void getWarehouseByCode_Success() {
        // Given
        when(warehouseRepository.findByCode("WH-001")).thenReturn(Optional.of(warehouse));

        // When
        ApiResponse<WarehouseResponseDto> response = warehouseService.getWarehouseByCode("WH-001");

        // Then
        assertNotNull(response);
        assertEquals("Warehouse retrieved successfully", response.getMessage());
        assertEquals("WH-001", response.getData().getCode());

        verify(warehouseRepository).findByCode("WH-001");
    }

    @Test
    void getWarehouseByCode_NotFound_ThrowsException() {
        // Given
        when(warehouseRepository.findByCode("INVALID")).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> warehouseService.getWarehouseByCode("INVALID")
        );

        assertTrue(exception.getMessage().contains("Warehouse"));
        assertTrue(exception.getMessage().contains("code"));

        verify(warehouseRepository).findByCode("INVALID");
    }

    @Test
    void getAllWarehouses_Success() {
        // Given
        Warehouse warehouse2 = Warehouse.builder()
                .id(2L)
                .name("Secondary Warehouse")
                .code("WH-002")
                .capacity(5000)
                .manager(manager)
                .active(true)
                .build();

        List<Warehouse> warehouses = Arrays.asList(warehouse, warehouse2);
        when(warehouseRepository.findAll()).thenReturn(warehouses);

        // When
        ApiResponse<List<WarehouseResponseDto>> response = warehouseService.getAllWarehouses();

        // Then
        assertNotNull(response);
        assertEquals("Warehouses retrieved successfully", response.getMessage());
        assertEquals(2, response.getData().size());
        assertEquals("WH-001", response.getData().get(0).getCode());
        assertEquals("WH-002", response.getData().get(1).getCode());

        verify(warehouseRepository).findAll();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // UPDATE WAREHOUSE TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void updateWarehouse_Success() {
        // Given
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(userRepository.findById(1L)).thenReturn(Optional.of(manager));
        when(warehouseRepository.save(any(Warehouse.class))).thenReturn(warehouse);

        // When
        ApiResponse<WarehouseResponseDto> response = warehouseService.updateWarehouse(1L, requestDto);

        // Then
        assertNotNull(response);
        assertEquals("Warehouse updated successfully", response.getMessage());

        verify(warehouseRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(warehouseRepository).save(any(Warehouse.class));
    }

    @Test
    void updateWarehouse_NotFound_ThrowsException() {
        // Given
        when(warehouseRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> warehouseService.updateWarehouse(999L, requestDto)
        );

        assertTrue(exception.getMessage().contains("Warehouse"));

        verify(warehouseRepository).findById(999L);
        verify(warehouseRepository, never()).save(any());
    }

    @Test
    void updateWarehouse_DuplicateCode_ThrowsException() {
        // Given
        requestDto.setCode("WH-002"); // Different code
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(warehouseRepository.existsByCode("WH-002")).thenReturn(true);

        // When & Then
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> warehouseService.updateWarehouse(1L, requestDto)
        );

        assertTrue(exception.getMessage().contains("code"));

        verify(warehouseRepository).findById(1L);
        verify(warehouseRepository).existsByCode("WH-002");
        verify(warehouseRepository, never()).save(any());
    }

    @Test
    void updateWarehouse_ManagerNotFound_ThrowsException() {
        // Given
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        requestDto.setManagerId(999L);

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> warehouseService.updateWarehouse(1L, requestDto)
        );

        assertTrue(exception.getMessage().contains("User"));

        verify(warehouseRepository).findById(1L);
        verify(userRepository).findById(999L);
        verify(warehouseRepository, never()).save(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // DELETE WAREHOUSE TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void deleteWarehouse_Success() {
        // Given
        when(warehouseRepository.existsById(1L)).thenReturn(true);
        doNothing().when(warehouseRepository).deleteById(1L);

        // When
        ApiResponse<Void> response = warehouseService.deleteWarehouse(1L);

        // Then
        assertNotNull(response);
        assertEquals("Warehouse deleted successfully", response.getMessage());

        verify(warehouseRepository).existsById(1L);
        verify(warehouseRepository).deleteById(1L);
    }

    @Test
    void deleteWarehouse_NotFound_ThrowsException() {
        // Given
        when(warehouseRepository.existsById(999L)).thenReturn(false);

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> warehouseService.deleteWarehouse(999L)
        );

        assertTrue(exception.getMessage().contains("Warehouse"));

        verify(warehouseRepository).existsById(999L);
        verify(warehouseRepository, never()).deleteById(anyLong());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // COUNT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void countWarehouses_Success() {
        // Given
        when(warehouseRepository.count()).thenReturn(10L);

        // When
        ApiResponse<Long> response = warehouseService.countWarehouses();

        // Then
        assertNotNull(response);
        assertEquals("Total warehouses counted successfully", response.getMessage());
        assertEquals(10L, response.getData());

        verify(warehouseRepository).count();
    }
}