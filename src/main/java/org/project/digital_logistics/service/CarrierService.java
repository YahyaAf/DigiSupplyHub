package org.project.digital_logistics.service;

import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.carrier.CarrierRequestDto;
import org.project.digital_logistics.dto.carrier.CarrierResponseDto;
import org.project.digital_logistics.model.enums.CarrierStatus;
import org.project.digital_logistics.exception.DuplicateResourceException;
import org.project.digital_logistics.exception.InvalidOperationException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.mapper.CarrierMapper;
import org.project.digital_logistics.model.Carrier;
import org.project.digital_logistics.repository.CarrierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CarrierService {

    private final CarrierRepository carrierRepository;

    @Autowired
    public CarrierService(CarrierRepository carrierRepository) {
        this.carrierRepository = carrierRepository;
    }

    @Transactional
    public ApiResponse<CarrierResponseDto> createCarrier(CarrierRequestDto requestDto) {
        if (carrierRepository.existsByCode(requestDto.getCode())) {
            throw new DuplicateResourceException("Carrier", "code", requestDto.getCode());
        }

        Carrier carrier = CarrierMapper.toEntity(requestDto);
        Carrier savedCarrier = carrierRepository.save(carrier);

        CarrierResponseDto responseDto = CarrierMapper.toResponseDto(savedCarrier);
        return new ApiResponse<>("Carrier created successfully", responseDto);
    }

    public ApiResponse<CarrierResponseDto> getCarrierById(Long id) {
        Carrier carrier = carrierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carrier", "id", id));

        CarrierResponseDto responseDto = CarrierMapper.toResponseDto(carrier);
        return new ApiResponse<>("Carrier retrieved successfully", responseDto);
    }

    public ApiResponse<CarrierResponseDto> getCarrierByCode(String code) {
        Carrier carrier = carrierRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Carrier", "code", code));

        CarrierResponseDto responseDto = CarrierMapper.toResponseDto(carrier);
        return new ApiResponse<>("Carrier retrieved successfully", responseDto);
    }

    public ApiResponse<List<CarrierResponseDto>> getAllCarriers() {
        List<CarrierResponseDto> carriers = carrierRepository.findAll()
                .stream()
                .map(CarrierMapper::toResponseDto)
                .toList();
        return new ApiResponse<>("Carriers retrieved successfully", carriers);
    }

    public ApiResponse<List<CarrierResponseDto>> getCarriersByStatus(CarrierStatus status) {
        List<CarrierResponseDto> carriers = carrierRepository.findByStatus(status)
                .stream()
                .map(CarrierMapper::toResponseDto)
                .toList();
        return new ApiResponse<>("Carriers retrieved successfully", carriers);
    }

    public ApiResponse<List<CarrierResponseDto>> getAvailableCarriers() {
        List<CarrierResponseDto> carriers = carrierRepository.findAvailableCarriers()
                .stream()
                .map(CarrierMapper::toResponseDto)
                .toList();
        return new ApiResponse<>("Available carriers retrieved successfully", carriers);
    }

    @Transactional
    public ApiResponse<CarrierResponseDto> updateCarrier(Long id, CarrierRequestDto requestDto) {
        Carrier carrier = carrierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carrier", "id", id));

        if (!carrier.getCode().equals(requestDto.getCode()) &&
                carrierRepository.existsByCode(requestDto.getCode())) {
            throw new DuplicateResourceException("Carrier", "code", requestDto.getCode());
        }

        CarrierMapper.updateEntityFromDto(requestDto, carrier);
        Carrier savedCarrier = carrierRepository.save(carrier);

        CarrierResponseDto responseDto = CarrierMapper.toResponseDto(savedCarrier);
        return new ApiResponse<>("Carrier updated successfully", responseDto);
    }

    @Transactional
    public ApiResponse<CarrierResponseDto> updateCarrierStatus(Long id, CarrierStatus status) {
        Carrier carrier = carrierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carrier", "id", id));

        carrier.setStatus(status);
        Carrier savedCarrier = carrierRepository.save(carrier);

        CarrierResponseDto responseDto = CarrierMapper.toResponseDto(savedCarrier);
        return new ApiResponse<>("Carrier status updated to " + status, responseDto);
    }

    @Transactional
    public ApiResponse<Void> deleteCarrier(Long id) {
        Carrier carrier = carrierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carrier", "id", id));

        if (!carrier.getShipments().isEmpty()) {
            throw new InvalidOperationException(
                    "Cannot delete carrier with assigned shipments. Total shipments: " + carrier.getShipments().size()
            );
        }

        carrierRepository.deleteById(id);
        return new ApiResponse<>("Carrier deleted successfully", null);
    }

    public ApiResponse<Long> countCarriers() {
        long count = carrierRepository.count();
        return new ApiResponse<>("Total carriers counted successfully", count);
    }

    @Transactional
    public void incrementDailyShipments(Long carrierId) {
        Carrier carrier = carrierRepository.findById(carrierId)
                .orElseThrow(() -> new ResourceNotFoundException("Carrier", "id", carrierId));

        carrier.setCurrentDailyShipments(carrier.getCurrentDailyShipments() + 1);
        carrierRepository.save(carrier);
    }

    @Transactional
    public void decrementDailyShipments(Long carrierId) {
        Carrier carrier = carrierRepository.findById(carrierId)
                .orElseThrow(() -> new ResourceNotFoundException("Carrier", "id", carrierId));

        if (carrier.getCurrentDailyShipments() > 0) {
            carrier.setCurrentDailyShipments(carrier.getCurrentDailyShipments() - 1);
            carrierRepository.save(carrier);
        }
    }

    @Transactional
    public ApiResponse<Void> resetDailyShipments() {
        List<Carrier> carriers = carrierRepository.findAll();
        carriers.forEach(carrier -> carrier.setCurrentDailyShipments(0));
        carrierRepository.saveAll(carriers);

        return new ApiResponse<>("Daily shipments reset for all carriers", null);
    }
}