package org.project.digital_logistics.dto.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryResponseDto {

    private Long id;

    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;

    private Long productId;
    private String productSku;
    private String productName;

    private Integer qtyOnHand;
    private Integer qtyReserved;
    private Integer qtyAvailable;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}