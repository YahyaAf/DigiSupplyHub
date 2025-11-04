package org.project.digital_logistics.service;

import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.inventorymovement.InventoryMovementRequestDto;
import org.project.digital_logistics.dto.inventorymovement.InventoryMovementResponseDto;
import org.project.digital_logistics.model.enums.MovementType;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.mapper.InventoryMovementMapper;
import org.project.digital_logistics.model.Inventory;
import org.project.digital_logistics.model.InventoryMovement;
import org.project.digital_logistics.repository.InventoryMovementRepository;
import org.project.digital_logistics.repository.InventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class InventoryMovementService {

    private final InventoryMovementRepository movementRepository;
    private final InventoryRepository inventoryRepository;

    @Autowired
    public InventoryMovementService(InventoryMovementRepository movementRepository,
                                    InventoryRepository inventoryRepository) {
        this.movementRepository = movementRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public ApiResponse<InventoryMovementResponseDto> createMovement(InventoryMovementRequestDto requestDto) {
        Inventory inventory = inventoryRepository.findById(requestDto.getInventoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "id", requestDto.getInventoryId()));

        InventoryMovement movement = InventoryMovementMapper.toEntity(requestDto, inventory);
        InventoryMovement savedMovement = movementRepository.save(movement);

        InventoryMovementResponseDto responseDto = InventoryMovementMapper.toResponseDto(savedMovement);

        return new ApiResponse<>("Inventory movement recorded successfully", responseDto);
    }

    @Transactional
    public void recordMovement(Long inventoryId, MovementType type, Integer quantity,
                               String referenceDocument, String description) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "id", inventoryId));

        InventoryMovement movement = InventoryMovement.builder()
                .inventory(inventory)
                .type(type)
                .quantity(quantity)
                .referenceDocument(referenceDocument)
                .description(description)
                .build();

        movementRepository.save(movement);
    }

    public ApiResponse<List<InventoryMovementResponseDto>> getAllMovements() {
        List<InventoryMovementResponseDto> movements = movementRepository.findAll()
                .stream()
                .map(InventoryMovementMapper::toResponseDto)
                .toList();
        return new ApiResponse<>("Inventory movements retrieved successfully", movements);
    }

    public ApiResponse<List<InventoryMovementResponseDto>> getMovementsByInventory(Long inventoryId) {
        // Verify inventory exists
        if (!inventoryRepository.existsById(inventoryId)) {
            throw new ResourceNotFoundException("Inventory", "id", inventoryId);
        }

        List<InventoryMovementResponseDto> movements = movementRepository.findByInventoryId(inventoryId)
                .stream()
                .map(InventoryMovementMapper::toResponseDto)
                .toList();
        return new ApiResponse<>("Inventory movements retrieved successfully", movements);
    }

    public ApiResponse<List<InventoryMovementResponseDto>> getMovementsByWarehouse(Long warehouseId) {
        List<InventoryMovementResponseDto> movements = movementRepository.findByWarehouseId(warehouseId)
                .stream()
                .map(InventoryMovementMapper::toResponseDto)
                .toList();
        return new ApiResponse<>("Warehouse movements retrieved successfully", movements);
    }
}