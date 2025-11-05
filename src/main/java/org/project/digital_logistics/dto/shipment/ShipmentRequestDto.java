package org.project.digital_logistics.dto.shipment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentRequestDto {

    @NotNull(message = "Sales Order ID is required")
    @Positive(message = "Sales Order ID must be positive")
    private Long salesOrderId;

    private String trackingNumber;

    private LocalDateTime plannedDate;
}