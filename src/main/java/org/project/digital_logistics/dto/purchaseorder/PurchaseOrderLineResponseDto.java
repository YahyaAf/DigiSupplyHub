package org.project.digital_logistics.dto.purchaseorder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderLineResponseDto {

    private Long id;

    // Product info
    private Long productId;
    private String productSku;
    private String productName;

    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice; // quantity * unitPrice
}