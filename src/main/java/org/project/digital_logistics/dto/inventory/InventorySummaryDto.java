package org.project.digital_logistics.dto.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventorySummaryDto {

    private Long productId;
    private String productSku;
    private String productName;
    private Integer totalStock;
    private Integer totalReserved;
    private Integer totalAvailable;
    private Integer warehouseCount;
}