package org.project.digital_logistics.dto.inventory;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryRequestDto {

    @NotNull(message = "Warehouse ID is required")
    @Positive(message = "Warehouse ID must be positive")
    private Long warehouseId;

    @NotNull(message = "Product ID is required")
    @Positive(message = "Product ID must be positive")
    private Long productId;

    @NotNull(message = "Quantity on hand is required")
    @Min(value = 0, message = "Quantity on hand must be zero or positive")
    private Integer qtyOnHand;

    @NotNull(message = "Quantity reserved is required")
    @Min(value = 0, message = "Quantity reserved must be zero or positive")
    private Integer qtyReserved;
}