package org.project.digital_logistics.mapper;

import org.project.digital_logistics.dto.inventorymovement.InventoryMovementRequestDto;
import org.project.digital_logistics.dto.inventorymovement.InventoryMovementResponseDto;
import org.project.digital_logistics.model.Inventory;
import org.project.digital_logistics.model.InventoryMovement;

public class InventoryMovementMapper {

    private InventoryMovementMapper() {
        throw new IllegalStateException("Utility class");
    }

    public static InventoryMovement toEntity(InventoryMovementRequestDto dto, Inventory inventory) {
        if (dto == null) {
            return null;
        }

        return InventoryMovement.builder()
                .inventory(inventory)
                .type(dto.getType())
                .quantity(dto.getQuantity())
                .occurredAt(dto.getOccurredAt())
                .referenceDocument(dto.getReferenceDocument())
                .description(dto.getDescription())
                .build();
    }

    public static InventoryMovementResponseDto toResponseDto(InventoryMovement movement) {
        if (movement == null) {
            return null;
        }

        Inventory inventory = movement.getInventory();

        return InventoryMovementResponseDto.builder()
                .id(movement.getId())
                .inventoryId(inventory != null ? inventory.getId() : null)
                .warehouseId(inventory != null && inventory.getWarehouse() != null ?
                        inventory.getWarehouse().getId() : null)
                .warehouseCode(inventory != null && inventory.getWarehouse() != null ?
                        inventory.getWarehouse().getCode() : null)
                .warehouseName(inventory != null && inventory.getWarehouse() != null ?
                        inventory.getWarehouse().getName() : null)
                .productId(inventory != null && inventory.getProduct() != null ?
                        inventory.getProduct().getId() : null)
                .productSku(inventory != null && inventory.getProduct() != null ?
                        inventory.getProduct().getSku() : null)
                .productName(inventory != null && inventory.getProduct() != null ?
                        inventory.getProduct().getName() : null)
                .type(movement.getType())
                .quantity(movement.getQuantity())
                .occurredAt(movement.getOccurredAt())
                .referenceDocument(movement.getReferenceDocument())
                .description(movement.getDescription())
                .build();
    }
}