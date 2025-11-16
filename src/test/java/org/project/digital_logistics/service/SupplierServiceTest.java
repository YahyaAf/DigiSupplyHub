package org.project.digital_logistics.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.supplier.SupplierRequestDto;
import org.project.digital_logistics.dto.supplier.SupplierResponseDto;
import org.project.digital_logistics.exception.DuplicateResourceException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.model.Supplier;
import org.project.digital_logistics.repository.SupplierRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @InjectMocks
    private SupplierService supplierService;

    private Supplier supplier;
    private SupplierRequestDto requestDto;

    @BeforeEach
    void setUp() {
        // Setup SupplierRequestDto
        requestDto = new SupplierRequestDto();
        requestDto.setName("Test Supplier");
        requestDto.setPhoneNumber("0612345678");
        requestDto.setAddress("123 Supplier Street, Casablanca");
        requestDto.setMatricule("SUP-001");

        // Setup Supplier Entity
        supplier = Supplier.builder()
                .id(1L)
                .name("Test Supplier")
                .phoneNumber("0612345678")
                .address("123 Supplier Street, Casablanca")
                .matricule("SUP-001")
                .build();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CREATE SUPPLIER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void createSupplier_Success() {
        // Given
        when(supplierRepository.existsByMatricule("SUP-001")).thenReturn(false);
        when(supplierRepository.save(any(Supplier.class))).thenReturn(supplier);

        // When
        ApiResponse<SupplierResponseDto> response = supplierService.createSupplier(requestDto);

        // Then
        assertNotNull(response);
        assertEquals("Supplier created successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals("SUP-001", response.getData().getMatricule());
        assertEquals("Test Supplier", response.getData().getName());

        verify(supplierRepository).existsByMatricule("SUP-001");
        verify(supplierRepository).save(any(Supplier.class));
    }

    @Test
    void createSupplier_DuplicateMatricule_ThrowsException() {
        // Given
        when(supplierRepository.existsByMatricule("SUP-001")).thenReturn(true);

        // When & Then
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> supplierService.createSupplier(requestDto)
        );

        assertTrue(exception.getMessage().contains("matricule"));
        assertTrue(exception.getMessage().contains("SUP-001"));

        verify(supplierRepository).existsByMatricule("SUP-001");
        verify(supplierRepository, never()).save(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // GET SUPPLIER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getSupplierById_Success() {
        // Given
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        // When
        ApiResponse<SupplierResponseDto> response = supplierService.getSupplierById(1L);

        // Then
        assertNotNull(response);
        assertEquals("Supplier retrieved successfully", response.getMessage());
        assertEquals(1L, response.getData().getId());
        assertEquals("SUP-001", response.getData().getMatricule());

        verify(supplierRepository).findById(1L);
    }

    @Test
    void getSupplierById_NotFound_ThrowsException() {
        // Given
        when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> supplierService.getSupplierById(999L)
        );

        assertTrue(exception.getMessage().contains("Supplier"));
        assertTrue(exception.getMessage().contains("id"));

        verify(supplierRepository).findById(999L);
    }

    @Test
    void getAllSuppliers_Success() {
        // Given
        Supplier supplier2 = Supplier.builder()
                .id(2L)
                .name("Second Supplier")
                .phoneNumber("0698765432")
                .address("456 Another Street")
                .matricule("SUP-002")
                .build();

        List<Supplier> suppliers = Arrays.asList(supplier, supplier2);
        when(supplierRepository.findAll()).thenReturn(suppliers);

        // When
        ApiResponse<List<SupplierResponseDto>> response = supplierService.getAllSuppliers();

        // Then
        assertNotNull(response);
        assertEquals("Suppliers retrieved successfully", response.getMessage());
        assertEquals(2, response.getData().size());
        assertEquals("SUP-001", response.getData().get(0).getMatricule());
        assertEquals("SUP-002", response.getData().get(1).getMatricule());

        verify(supplierRepository).findAll();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // UPDATE SUPPLIER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void updateSupplier_Success() {
        // Given
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(supplierRepository.save(any(Supplier.class))).thenReturn(supplier);

        // When
        ApiResponse<SupplierResponseDto> response = supplierService.updateSupplier(1L, requestDto);

        // Then
        assertNotNull(response);
        assertEquals("Supplier updated successfully", response.getMessage());

        verify(supplierRepository).findById(1L);
        verify(supplierRepository).save(any(Supplier.class));
    }

    @Test
    void updateSupplier_NotFound_ThrowsException() {
        // Given
        when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> supplierService.updateSupplier(999L, requestDto)
        );

        assertTrue(exception.getMessage().contains("Supplier"));

        verify(supplierRepository).findById(999L);
        verify(supplierRepository, never()).save(any());
    }

    @Test
    void updateSupplier_DuplicateMatricule_ThrowsException() {
        // Given
        requestDto.setMatricule("SUP-002"); // Different matricule
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(supplierRepository.existsByMatricule("SUP-002")).thenReturn(true);

        // When & Then
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> supplierService.updateSupplier(1L, requestDto)
        );

        assertTrue(exception.getMessage().contains("matricule"));

        verify(supplierRepository).findById(1L);
        verify(supplierRepository).existsByMatricule("SUP-002");
        verify(supplierRepository, never()).save(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // DELETE SUPPLIER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void deleteSupplier_Success() {
        // Given
        when(supplierRepository.existsById(1L)).thenReturn(true);
        doNothing().when(supplierRepository).deleteById(1L);

        // When
        ApiResponse<Void> response = supplierService.deleteSupplier(1L);

        // Then
        assertNotNull(response);
        assertEquals("Supplier deleted successfully", response.getMessage());

        verify(supplierRepository).existsById(1L);
        verify(supplierRepository).deleteById(1L);
    }

    @Test
    void deleteSupplier_NotFound_ThrowsException() {
        // Given
        when(supplierRepository.existsById(999L)).thenReturn(false);

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> supplierService.deleteSupplier(999L)
        );

        assertTrue(exception.getMessage().contains("Supplier"));

        verify(supplierRepository).existsById(999L);
        verify(supplierRepository, never()).deleteById(anyLong());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // COUNT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void countSuppliers_Success() {
        // Given
        when(supplierRepository.count()).thenReturn(15L);

        // When
        ApiResponse<Long> response = supplierService.countSuppliers();

        // Then
        assertNotNull(response);
        assertEquals("Total suppliers counted successfully", response.getMessage());
        assertEquals(15L, response.getData());

        verify(supplierRepository).count();
    }
}