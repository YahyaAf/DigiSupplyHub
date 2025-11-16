package org.project.digital_logistics.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.carrier.CarrierRequestDto;
import org.project.digital_logistics.dto.carrier.CarrierResponseDto;
import org.project.digital_logistics.exception.DuplicateResourceException;
import org.project.digital_logistics.exception.InvalidOperationException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.model.Carrier;
import org.project.digital_logistics.model.Shipment;
import org.project.digital_logistics.model.enums.CarrierStatus;
import org.project.digital_logistics.repository.CarrierRepository;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarrierServiceTest {

    @Mock
    private CarrierRepository carrierRepository;

    @InjectMocks
    private CarrierService carrierService;

    private Carrier carrier;
    private CarrierRequestDto requestDto;

    @BeforeEach
    void setUp() {
        // Setup CarrierRequestDto
        requestDto = CarrierRequestDto.builder()
                .code("DHL-001")
                .name("DHL Express")
                .contactEmail("contact@dhl.com")
                .contactPhone("0612345678")
                .baseShippingRate(BigDecimal.valueOf(50))
                .maxDailyCapacity(100)
                .cutOffTime(LocalTime.of(17, 0))
                .build();

        // ✅ Setup Carrier Entity with MUTABLE list
        carrier = Carrier.builder()
                .id(1L)
                .code("DHL-001")
                .name("DHL Express")
                .contactEmail("contact@dhl.com")
                .contactPhone("0612345678")
                .baseShippingRate(BigDecimal.valueOf(50))
                .maxDailyCapacity(100)
                .currentDailyShipments(0)
                .cutOffTime(LocalTime.of(17, 0))
                .status(CarrierStatus.ACTIVE)
                .shipments(new ArrayList<>())  // ✅ Mutable list
                .build();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CREATE CARRIER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void createCarrier_Success() {
        // Given
        when(carrierRepository.existsByCode("DHL-001")).thenReturn(false);
        when(carrierRepository.save(any(Carrier.class))).thenReturn(carrier);

        // When
        ApiResponse<CarrierResponseDto> response = carrierService.createCarrier(requestDto);

        // Then
        assertNotNull(response);
        assertEquals("Carrier created successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals("DHL-001", response.getData().getCode());

        verify(carrierRepository).existsByCode("DHL-001");
        verify(carrierRepository).save(any(Carrier.class));
    }

    @Test
    void createCarrier_DuplicateCode_ThrowsException() {
        // Given
        when(carrierRepository.existsByCode("DHL-001")).thenReturn(true);

        // When & Then
        assertThrows(DuplicateResourceException.class,
                () -> carrierService.createCarrier(requestDto));

        verify(carrierRepository).existsByCode("DHL-001");
        verify(carrierRepository, never()).save(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // GET CARRIER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getCarrierById_Success() {
        // Given
        when(carrierRepository.findById(1L)).thenReturn(Optional.of(carrier));

        // When
        ApiResponse<CarrierResponseDto> response = carrierService.getCarrierById(1L);

        // Then
        assertNotNull(response);
        assertEquals("Carrier retrieved successfully", response.getMessage());
        assertEquals(1L, response.getData().getId());
        assertEquals("DHL-001", response.getData().getCode());
        verify(carrierRepository).findById(1L);
    }

    @Test
    void getCarrierById_NotFound_ThrowsException() {
        // Given
        when(carrierRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> carrierService.getCarrierById(999L));

        verify(carrierRepository).findById(999L);
    }

    @Test
    void getCarrierByCode_Success() {
        // Given
        when(carrierRepository.findByCode("DHL-001")).thenReturn(Optional.of(carrier));

        // When
        ApiResponse<CarrierResponseDto> response = carrierService.getCarrierByCode("DHL-001");

        // Then
        assertNotNull(response);
        assertEquals("Carrier retrieved successfully", response.getMessage());
        assertEquals("DHL-001", response.getData().getCode());
        verify(carrierRepository).findByCode("DHL-001");
    }

    @Test
    void getCarrierByCode_NotFound_ThrowsException() {
        // Given
        when(carrierRepository.findByCode("INVALID")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> carrierService.getCarrierByCode("INVALID"));

        verify(carrierRepository).findByCode("INVALID");
    }

    @Test
    void getAllCarriers_Success() {
        // Given
        List<Carrier> carriers = Arrays.asList(carrier);
        when(carrierRepository.findAll()).thenReturn(carriers);

        // When
        ApiResponse<List<CarrierResponseDto>> response = carrierService.getAllCarriers();

        // Then
        assertNotNull(response);
        assertEquals("Carriers retrieved successfully", response.getMessage());
        assertEquals(1, response.getData().size());
        assertEquals("DHL-001", response.getData().get(0).getCode());
        verify(carrierRepository).findAll();
    }

    @Test
    void getCarriersByStatus_Success() {
        // Given
        List<Carrier> carriers = Arrays.asList(carrier);
        when(carrierRepository.findByStatus(CarrierStatus.ACTIVE)).thenReturn(carriers);

        // When
        ApiResponse<List<CarrierResponseDto>> response =
                carrierService.getCarriersByStatus(CarrierStatus.ACTIVE);

        // Then
        assertNotNull(response);
        assertEquals("Carriers retrieved successfully", response.getMessage());
        assertEquals(1, response.getData().size());
        verify(carrierRepository).findByStatus(CarrierStatus.ACTIVE);
    }

    @Test
    void getAvailableCarriers_Success() {
        // Given
        List<Carrier> carriers = Arrays.asList(carrier);
        when(carrierRepository.findAvailableCarriers()).thenReturn(carriers);

        // When
        ApiResponse<List<CarrierResponseDto>> response = carrierService.getAvailableCarriers();

        // Then
        assertNotNull(response);
        assertEquals("Available carriers retrieved successfully", response.getMessage());
        assertEquals(1, response.getData().size());
        verify(carrierRepository).findAvailableCarriers();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // UPDATE CARRIER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void updateCarrier_Success() {
        // Given
        when(carrierRepository.findById(1L)).thenReturn(Optional.of(carrier));
        // ✅ REMOVED: when(carrierRepository.existsByCode("DHL-001")).thenReturn(false);
        // Not needed because code is same (no duplicate check)
        when(carrierRepository.save(any(Carrier.class))).thenReturn(carrier);

        // When
        ApiResponse<CarrierResponseDto> response = carrierService.updateCarrier(1L, requestDto);

        // Then
        assertNotNull(response);
        assertEquals("Carrier updated successfully", response.getMessage());
        verify(carrierRepository).save(any(Carrier.class));
    }

    @Test
    void updateCarrier_NotFound_ThrowsException() {
        // Given
        when(carrierRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> carrierService.updateCarrier(999L, requestDto));

        verify(carrierRepository).findById(999L);
        verify(carrierRepository, never()).save(any());
    }

    @Test
    void updateCarrier_DuplicateCode_ThrowsException() {
        // Given
        requestDto.setCode("FEDEX-001");
        when(carrierRepository.findById(1L)).thenReturn(Optional.of(carrier));
        when(carrierRepository.existsByCode("FEDEX-001")).thenReturn(true);

        // When & Then
        assertThrows(DuplicateResourceException.class,
                () -> carrierService.updateCarrier(1L, requestDto));

        verify(carrierRepository).findById(1L);
        verify(carrierRepository).existsByCode("FEDEX-001");
        verify(carrierRepository, never()).save(any());
    }

    @Test
    void updateCarrierStatus_Success() {
        // Given
        when(carrierRepository.findById(1L)).thenReturn(Optional.of(carrier));
        when(carrierRepository.save(any(Carrier.class))).thenReturn(carrier);

        // When
        ApiResponse<CarrierResponseDto> response =
                carrierService.updateCarrierStatus(1L, CarrierStatus.SUSPENDED);

        // Then
        assertNotNull(response);
        assertTrue(response.getMessage().contains("SUSPENDED"));

        ArgumentCaptor<Carrier> captor = ArgumentCaptor.forClass(Carrier.class);
        verify(carrierRepository).save(captor.capture());

        assertEquals(CarrierStatus.SUSPENDED, captor.getValue().getStatus());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // DELETE CARRIER TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void deleteCarrier_NoShipments_Success() {
        // Given
        when(carrierRepository.findById(1L)).thenReturn(Optional.of(carrier));
        doNothing().when(carrierRepository).deleteById(1L);

        // When
        ApiResponse<Void> response = carrierService.deleteCarrier(1L);

        // Then
        assertNotNull(response);
        assertEquals("Carrier deleted successfully", response.getMessage());
        verify(carrierRepository).deleteById(1L);
    }

    @Test
    void deleteCarrier_WithShipments_ThrowsException() {
        // Given
        Shipment shipment = Shipment.builder()
                .id(1L)
                .trackingNumber("TRACK-123")
                .build();

        // ✅ FIXED: Use mutable list
        carrier.getShipments().add(shipment);

        when(carrierRepository.findById(1L)).thenReturn(Optional.of(carrier));

        // When & Then
        InvalidOperationException exception = assertThrows(InvalidOperationException.class,
                () -> carrierService.deleteCarrier(1L));

        assertTrue(exception.getMessage().contains("Cannot delete carrier with assigned shipments"));
        verify(carrierRepository).findById(1L);
        verify(carrierRepository, never()).deleteById(anyLong());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // COUNT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void countCarriers_Success() {
        // Given
        when(carrierRepository.count()).thenReturn(5L);

        // When
        ApiResponse<Long> response = carrierService.countCarriers();

        // Then
        assertNotNull(response);
        assertEquals(5L, response.getData());
        verify(carrierRepository).count();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // DAILY SHIPMENTS MANAGEMENT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void incrementDailyShipments_Success() {
        // Given
        carrier.setCurrentDailyShipments(10);
        when(carrierRepository.findById(1L)).thenReturn(Optional.of(carrier));
        when(carrierRepository.save(any(Carrier.class))).thenReturn(carrier);

        // When
        carrierService.incrementDailyShipments(1L);

        // Then
        ArgumentCaptor<Carrier> captor = ArgumentCaptor.forClass(Carrier.class);
        verify(carrierRepository).save(captor.capture());

        assertEquals(11, captor.getValue().getCurrentDailyShipments());
    }

    @Test
    void decrementDailyShipments_Success() {
        // Given
        carrier.setCurrentDailyShipments(10);
        when(carrierRepository.findById(1L)).thenReturn(Optional.of(carrier));
        when(carrierRepository.save(any(Carrier.class))).thenReturn(carrier);

        // When
        carrierService.decrementDailyShipments(1L);

        // Then
        ArgumentCaptor<Carrier> captor = ArgumentCaptor.forClass(Carrier.class);
        verify(carrierRepository).save(captor.capture());

        assertEquals(9, captor.getValue().getCurrentDailyShipments());
    }

    @Test
    void decrementDailyShipments_AlreadyZero_RemainsZero() {
        // Given
        carrier.setCurrentDailyShipments(0);
        when(carrierRepository.findById(1L)).thenReturn(Optional.of(carrier));
        // ✅ REMOVED: when(carrierRepository.save(...))
        // Not called because condition prevents save

        // When
        carrierService.decrementDailyShipments(1L);

        // Then
        verify(carrierRepository).findById(1L);
        verify(carrierRepository, never()).save(any());
    }

    @Test
    void resetDailyShipments_Success() {
        // Given
        Carrier carrier1 = Carrier.builder()
                .id(1L)
                .code("DHL-001")
                .name("DHL")
                .currentDailyShipments(25)
                .build();

        Carrier carrier2 = Carrier.builder()
                .id(2L)
                .code("FEDEX-001")
                .name("FedEx")
                .currentDailyShipments(30)
                .build();

        List<Carrier> carriers = Arrays.asList(carrier1, carrier2);

        when(carrierRepository.findAll()).thenReturn(carriers);
        when(carrierRepository.saveAll(anyList())).thenReturn(carriers);

        // When
        ApiResponse<Void> response = carrierService.resetDailyShipments();

        // Then
        assertNotNull(response);
        assertEquals("Daily shipments reset for all carriers", response.getMessage());

        verify(carrierRepository).findAll();
        verify(carrierRepository).saveAll(anyList());

        // Verify all carriers reset to 0
        carriers.forEach(c -> assertEquals(0, c.getCurrentDailyShipments()));
    }
}