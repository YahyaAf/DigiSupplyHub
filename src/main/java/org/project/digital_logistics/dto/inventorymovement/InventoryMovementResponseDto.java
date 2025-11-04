package org.project.digital_logistics.dto.inventorymovement;

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
public class InventoryMovementResponseDto {

    private Long id;

    private Long inventoryId;

    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;

    private Long productId;
    private String productSku;
    private String productName;

    private MovementType type;
    private Integer quantity;
    private LocalDateTime occurredAt;
    private String referenceDocument;
    private String description;
}