package org.project.digital_logistics.mapper;

import org.project.digital_logistics.dto.inventory.InventoryRequestDto;
import org.project.digital_logistics.dto.inventory.InventoryResponseDto;
import org.project.digital_logistics.model.Inventory;
import org.project.digital_logistics.model.Product;
import org.project.digital_logistics.model.Warehouse;

public class InventoryMapper {

    private InventoryMapper() {
        throw new IllegalStateException("Utility class");
    }

    public static Inventory toEntity(InventoryRequestDto dto, Warehouse warehouse, Product product) {
        if (dto == null) {
            return null;
        }

        return Inventory.builder()
                .warehouse(warehouse)
                .product(product)
                .qtyOnHand(dto.getQtyOnHand())
                .qtyReserved(dto.getQtyReserved())
                .build();
    }

    public static InventoryResponseDto toResponseDto(Inventory inventory) {
        if (inventory == null) {
            return null;
        }

        return InventoryResponseDto.builder()
                .id(inventory.getId())
                // Warehouse info
                .warehouseId(inventory.getWarehouse() != null ? inventory.getWarehouse().getId() : null)
                .warehouseCode(inventory.getWarehouse() != null ? inventory.getWarehouse().getCode() : null)
                .warehouseName(inventory.getWarehouse() != null ? inventory.getWarehouse().getName() : null)
                // Product info
                .productId(inventory.getProduct() != null ? inventory.getProduct().getId() : null)
                .productSku(inventory.getProduct() != null ? inventory.getProduct().getSku() : null)
                .productName(inventory.getProduct() != null ? inventory.getProduct().getName() : null)
                // Quantities
                .qtyOnHand(inventory.getQtyOnHand())
                .qtyReserved(inventory.getQtyReserved())
                .qtyAvailable(inventory.getAvailableQuantity())
                // Timestamps
                .createdAt(inventory.getCreatedAt())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }

    public static void updateEntityFromDto(InventoryRequestDto dto, Inventory inventory,
                                           Warehouse warehouse, Product product) {
        if (dto == null || inventory == null) {
            return;
        }

        if (warehouse != null) {
            inventory.setWarehouse(warehouse);
        }
        if (product != null) {
            inventory.setProduct(product);
        }
        if (dto.getQtyOnHand() != null) {
            inventory.setQtyOnHand(dto.getQtyOnHand());
        }
        if (dto.getQtyReserved() != null) {
            inventory.setQtyReserved(dto.getQtyReserved());
        }
    }

    public static void updateQuantities(Inventory inventory, Integer qtyOnHand, Integer qtyReserved) {
        if (inventory == null) {
            return;
        }
        if (qtyOnHand != null) {
            inventory.setQtyOnHand(qtyOnHand);
        }
        if (qtyReserved != null) {
            inventory.setQtyReserved(qtyReserved);
        }
    }
}