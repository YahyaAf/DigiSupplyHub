package org.project.digital_logistics.dto.purchaseorder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.project.digital_logistics.enums.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderResponseDto {

    private Long id;

    // Supplier info
    private Long supplierId;
    private String supplierName;
    private String supplierContactInfo;
    private String supplierMarticule;

    private PurchaseOrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expectedDelivery;
    private LocalDateTime approvedAt;
    private LocalDateTime receivedAt;
    private LocalDateTime canceledAt;

    // Order lines
    private List<PurchaseOrderLineResponseDto> orderLines;

    private BigDecimal totalAmount;  // Sum of all lines
    private Integer totalItems;      // Sum of all quantities
}