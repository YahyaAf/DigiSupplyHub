package org.project.digital_logistics.dto.purchaseorder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderRequestDto {

    @NotNull(message = "Supplier ID is required")
    @Positive(message = "Supplier ID must be positive")
    private Long supplierId;

    private LocalDateTime expectedDelivery;

    @NotNull(message = "Order lines are required")
    @NotEmpty(message = "Order must have at least one line")
    @Valid
    private List<PurchaseOrderLineDto> orderLines;
}