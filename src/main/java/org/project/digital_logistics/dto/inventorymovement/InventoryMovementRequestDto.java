package org.project.digital_logistics.dto.inventorymovement;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.project.digital_logistics.model.enums.MovementType;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryMovementRequestDto {

    @NotNull(message = "Inventory ID is required")
    @Positive(message = "Inventory ID must be positive")
    private Long inventoryId;

    @NotNull(message = "Movement type is required")
    private MovementType type;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    private LocalDateTime occurredAt;  // Optional, defaults to now

    @Size(max = 255, message = "Reference document must not exceed 255 characters")
    private String referenceDocument;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
}