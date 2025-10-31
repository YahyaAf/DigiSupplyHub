package org.project.digital_logistics.service;

import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.warehouse.WarehouseRequestDto;
import org.project.digital_logistics.dto.warehouse.WarehouseResponseDto;
import org.project.digital_logistics.exception.DuplicateResourceException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.mapper.WarehouseMapper;
import org.project.digital_logistics.model.User;
import org.project.digital_logistics.model.Warehouse;
import org.project.digital_logistics.repository.UserRepository;
import org.project.digital_logistics.repository.WarehouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;

    @Autowired
    public WarehouseService(WarehouseRepository warehouseRepository,
                            UserRepository userRepository) {
        this.warehouseRepository = warehouseRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ApiResponse<WarehouseResponseDto> createWarehouse(WarehouseRequestDto requestDto) {
        // 1. Check duplicate code
        if (warehouseRepository.existsByCode(requestDto.getCode())) {
            throw new DuplicateResourceException("Warehouse", "code", requestDto.getCode());
        }

        User manager = userRepository.findById(requestDto.getManagerId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", requestDto.getManagerId()));

        Warehouse warehouse = WarehouseMapper.toEntity(requestDto, manager);
        Warehouse savedWarehouse = warehouseRepository.save(warehouse);

        WarehouseResponseDto responseDto = WarehouseMapper.toResponseDto(savedWarehouse);

        return new ApiResponse<>("Warehouse created successfully", responseDto);
    }

    public ApiResponse<WarehouseResponseDto> getWarehouseById(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", id));

        WarehouseResponseDto responseDto = WarehouseMapper.toResponseDto(warehouse);
        return new ApiResponse<>("Warehouse retrieved successfully", responseDto);
    }

    public ApiResponse<WarehouseResponseDto> getWarehouseByCode(String code) {
        Warehouse warehouse = warehouseRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "code", code));

        WarehouseResponseDto responseDto = WarehouseMapper.toResponseDto(warehouse);
        return new ApiResponse<>("Warehouse retrieved successfully", responseDto);
    }

    public ApiResponse<List<WarehouseResponseDto>> getAllWarehouses() {
        List<WarehouseResponseDto> warehouses = warehouseRepository.findAll()
                .stream()
                .map(WarehouseMapper::toResponseDto)
                .toList();
        return new ApiResponse<>("Warehouses retrieved successfully", warehouses);
    }

    @Transactional
    public ApiResponse<WarehouseResponseDto> updateWarehouse(Long id, WarehouseRequestDto requestDto) {
        // 1. Find warehouse
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", id));

        // 2. Check duplicate code
        if (!warehouse.getCode().equals(requestDto.getCode()) &&
                warehouseRepository.existsByCode(requestDto.getCode())) {
            throw new DuplicateResourceException("Warehouse", "code", requestDto.getCode());
        }

        User manager = userRepository.findById(requestDto.getManagerId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", requestDto.getManagerId()));

        WarehouseMapper.updateEntityFromDto(requestDto, warehouse, manager);
        Warehouse savedWarehouse = warehouseRepository.save(warehouse);

        WarehouseResponseDto responseDto = WarehouseMapper.toResponseDto(savedWarehouse);

        return new ApiResponse<>("Warehouse updated successfully", responseDto);
    }

    @Transactional
    public ApiResponse<Void> deleteWarehouse(Long id) {
        if (!warehouseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Warehouse", "id", id);
        }

        warehouseRepository.deleteById(id);
        return new ApiResponse<>("Warehouse deleted successfully", null);
    }

    public ApiResponse<Long> countWarehouses() {
        long count = warehouseRepository.count();
        return new ApiResponse<>("Total warehouses counted successfully", count);
    }

}