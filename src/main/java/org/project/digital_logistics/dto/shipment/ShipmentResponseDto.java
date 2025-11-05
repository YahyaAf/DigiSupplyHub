package org.project.digital_logistics.dto.shipment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.project.digital_logistics.model.enums.ShipmentStatus;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentResponseDto {

    private Long id;

    private Long salesOrderId;
    private String clientName;
    private String clientEmail;
    private String clientPhoneNumber;
    private String clientAddress;

    private String trackingNumber;
    private ShipmentStatus status;
    private LocalDateTime plannedDate;
    private LocalDateTime shippedDate;
    private LocalDateTime deliveredDate;
    private LocalDateTime createdAt;
}