package org.project.digital_logistics.dto.salesorder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesOrderLineResponseDto {

    private Long id;

    private Long productId;
    private String productSku;
    private String productName;

    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;

    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private Boolean backOrder;
}