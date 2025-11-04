package org.project.digital_logistics.dto.salesorder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.project.digital_logistics.model.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesOrderResponseDto {

    private Long id;

    private Long clientId;
    private String clientName;
    private String clientEmail;
    private String clientPhoneNumber;
    private String clientAddress;

    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;

    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime reservedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;

    private List<SalesOrderLineResponseDto> orderLines;

    private BigDecimal totalAmount;
    private Integer totalItems;
}