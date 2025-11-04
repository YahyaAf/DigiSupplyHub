package org.project.digital_logistics.service;

import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.inventory.InventoryRequestDto;
import org.project.digital_logistics.dto.inventory.InventoryResponseDto;
import org.project.digital_logistics.exception.DuplicateResourceException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.mapper.InventoryMapper;
import org.project.digital_logistics.model.Inventory;
import org.project.digital_logistics.model.Product;
import org.project.digital_logistics.model.Warehouse;
import org.project.digital_logistics.model.enums.MovementType;
import org.project.digital_logistics.repository.InventoryRepository;
import org.project.digital_logistics.repository.ProductRepository;
import org.project.digital_logistics.repository.WarehouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final InventoryMovementService movementService;

    @Autowired
    public InventoryService(InventoryRepository inventoryRepository,
                            WarehouseRepository warehouseRepository,
                            ProductRepository productRepository,
                            InventoryMovementService movementService) {
        this.inventoryRepository = inventoryRepository;
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
        this.movementService = movementService;
    }

    @Transactional
    public ApiResponse<InventoryResponseDto> createInventory(InventoryRequestDto requestDto) {
        // 1. Check if inventory already exists for this warehouse-product combination
        if (inventoryRepository.existsByWarehouseIdAndProductId(
                requestDto.getWarehouseId(), requestDto.getProductId())) {
            throw new DuplicateResourceException(
                    "Inventory already exists for warehouse " + requestDto.getWarehouseId() +
                            " and product " + requestDto.getProductId()
            );
        }

        // 2. Fetch Warehouse
        Warehouse warehouse = warehouseRepository.findById(requestDto.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", requestDto.getWarehouseId()));

        // 3. Fetch Product
        Product product = productRepository.findById(requestDto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", requestDto.getProductId()));

        // 4. Validate quantities
        validateQuantities(requestDto.getQtyOnHand(), requestDto.getQtyReserved());

        // 5. Create inventory
        Inventory inventory = InventoryMapper.toEntity(requestDto, warehouse, product);
        Inventory savedInventory = inventoryRepository.save(inventory);

        // 6. Convert to DTO
        InventoryResponseDto responseDto = InventoryMapper.toResponseDto(savedInventory);

        return new ApiResponse<>("Inventory created successfully", responseDto);
    }

    public ApiResponse<InventoryResponseDto> getInventoryById(Long id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "id", id));

        InventoryResponseDto responseDto = InventoryMapper.toResponseDto(inventory);
        return new ApiResponse<>("Inventory retrieved successfully", responseDto);
    }

    public ApiResponse<InventoryResponseDto> getInventoryByWarehouseAndProduct(Long warehouseId, Long productId) {
        Inventory inventory = inventoryRepository.findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for warehouse " + warehouseId + " and product " + productId
                ));

        InventoryResponseDto responseDto = InventoryMapper.toResponseDto(inventory);
        return new ApiResponse<>("Inventory retrieved successfully", responseDto);
    }

    public ApiResponse<List<InventoryResponseDto>> getAllInventories() {
        List<InventoryResponseDto> inventories = inventoryRepository.findAll()
                .stream()
                .map(InventoryMapper::toResponseDto)
                .toList();
        return new ApiResponse<>("Inventories retrieved successfully", inventories);
    }

    public ApiResponse<List<InventoryResponseDto>> getInventoriesByWarehouse(Long warehouseId) {
        // Verify warehouse exists
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new ResourceNotFoundException("Warehouse", "id", warehouseId);
        }

        List<InventoryResponseDto> inventories = inventoryRepository.findByWarehouseId(warehouseId)
                .stream()
                .map(InventoryMapper::toResponseDto)
                .toList();
        return new ApiResponse<>("Warehouse inventories retrieved successfully", inventories);
    }

    public ApiResponse<List<InventoryResponseDto>> getInventoriesByProduct(Long productId) {
        // Verify product exists
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", "id", productId);
        }

        List<InventoryResponseDto> inventories = inventoryRepository.findByProductId(productId)
                .stream()
                .map(InventoryMapper::toResponseDto)
                .toList();
        return new ApiResponse<>("Product inventories retrieved successfully", inventories);
    }

    public ApiResponse<List<InventoryResponseDto>> getLowStockInWarehouse(Long warehouseId, Integer threshold) {
        // Verify warehouse exists
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new ResourceNotFoundException("Warehouse", "id", warehouseId);
        }

        List<InventoryResponseDto> inventories = inventoryRepository
                .findLowStockInWarehouse(warehouseId, threshold != null ? threshold : 10)
                .stream()
                .map(InventoryMapper::toResponseDto)
                .toList();
        return new ApiResponse<>("Low stock items retrieved successfully", inventories);
    }

    @Transactional
    public ApiResponse<InventoryResponseDto> updateInventory(Long id, InventoryRequestDto requestDto) {
        // 1. Find inventory
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "id", id));

        // 2. Check if changing warehouse or product creates duplicate
        if (!inventory.getWarehouse().getId().equals(requestDto.getWarehouseId()) ||
                !inventory.getProduct().getId().equals(requestDto.getProductId())) {

            if (inventoryRepository.existsByWarehouseIdAndProductId(
                    requestDto.getWarehouseId(), requestDto.getProductId())) {
                throw new DuplicateResourceException(
                        "Inventory already exists for warehouse " + requestDto.getWarehouseId() +
                                " and product " + requestDto.getProductId()
                );
            }
        }

        // 3. Fetch Warehouse if changed
        Warehouse warehouse = null;
        if (!inventory.getWarehouse().getId().equals(requestDto.getWarehouseId())) {
            warehouse = warehouseRepository.findById(requestDto.getWarehouseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", requestDto.getWarehouseId()));
        }

        // 4. Fetch Product if changed
        Product product = null;
        if (!inventory.getProduct().getId().equals(requestDto.getProductId())) {
            product = productRepository.findById(requestDto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", requestDto.getProductId()));
        }

        // 5. Validate quantities
        validateQuantities(requestDto.getQtyOnHand(), requestDto.getQtyReserved());

        // 6. Update inventory
        InventoryMapper.updateEntityFromDto(requestDto, inventory, warehouse, product);
        Inventory savedInventory = inventoryRepository.save(inventory);

        // 7. Convert to DTO
        InventoryResponseDto responseDto = InventoryMapper.toResponseDto(savedInventory);

        return new ApiResponse<>("Inventory updated successfully", responseDto);
    }

    @Transactional
    public ApiResponse<InventoryResponseDto> adjustQuantities(Long id, Integer qtyOnHand, Integer qtyReserved) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "id", id));

        Integer oldQtyOnHand = inventory.getQtyOnHand();

        if (qtyOnHand != null && qtyReserved != null) {
            validateQuantities(qtyOnHand, qtyReserved);
        }

        InventoryMapper.updateQuantities(inventory, qtyOnHand, qtyReserved);
        Inventory savedInventory = inventoryRepository.save(inventory);

        if (qtyOnHand != null && !oldQtyOnHand.equals(qtyOnHand)) {
            Integer quantityDifference = qtyOnHand - oldQtyOnHand;

            movementService.recordMovement(
                    savedInventory.getId(),
                    MovementType.ADJUSTMENT,
                    Math.abs(quantityDifference), // Always positive
                    "ADJ-" + System.currentTimeMillis(),
                    "Inventory adjustment - " +
                            (quantityDifference > 0 ? "Added " : "Removed ") +
                            Math.abs(quantityDifference) + " units - " +
                            savedInventory.getProduct().getName()
            );
        }

        InventoryResponseDto responseDto = InventoryMapper.toResponseDto(savedInventory);
        return new ApiResponse<>("Inventory quantities adjusted successfully", responseDto);
    }

    @Transactional
    public ApiResponse<Void> deleteInventory(Long id) {
        if (!inventoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Inventory", "id", id);
        }

        inventoryRepository.deleteById(id);
        return new ApiResponse<>("Inventory deleted successfully", null);
    }

    @Transactional
    public ApiResponse<Void> deleteInventoryByWarehouseAndProduct(Long warehouseId, Long productId) {
        if (!inventoryRepository.existsByWarehouseIdAndProductId(warehouseId, productId)) {
            throw new ResourceNotFoundException(
                    "Inventory not found for warehouse " + warehouseId + " and product " + productId
            );
        }

        inventoryRepository.deleteByWarehouseIdAndProductId(warehouseId, productId);
        return new ApiResponse<>("Inventory deleted successfully", null);
    }

    public ApiResponse<Integer> getTotalStockByProduct(Long productId) {
        // Verify product exists
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", "id", productId);
        }

        Integer totalStock = inventoryRepository.getTotalStockByProduct(productId);
        return new ApiResponse<>("Total stock retrieved successfully", totalStock);
    }

    public ApiResponse<Integer> getAvailableStockByProduct(Long productId) {
        // Verify product exists
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", "id", productId);
        }

        Integer availableStock = inventoryRepository.getAvailableStockByProduct(productId);
        return new ApiResponse<>("Available stock retrieved successfully", availableStock);
    }

    public ApiResponse<Long> countInventories() {
        long count = inventoryRepository.count();
        return new ApiResponse<>("Total inventories counted successfully", count);
    }

    // Helper method: Validate quantities
    private void validateQuantities(Integer qtyOnHand, Integer qtyReserved) {
        if (qtyReserved > qtyOnHand) {
            throw new IllegalArgumentException(
                    "Reserved quantity (" + qtyReserved + ") cannot exceed quantity on hand (" + qtyOnHand + ")"
            );
        }
    }
}