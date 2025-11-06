package org.project.digital_logistics.mapper;

import org.project.digital_logistics.dto.shipment.ShipmentRequestDto;
import org.project.digital_logistics.dto.shipment.ShipmentResponseDto;
import org.project.digital_logistics.model.enums.ShipmentStatus;
import org.project.digital_logistics.model.SalesOrder;
import org.project.digital_logistics.model.Shipment;

import java.time.LocalDateTime;

public class ShipmentMapper {

    private ShipmentMapper() {
        throw new IllegalStateException("Utility class");
    }

    public static Shipment toEntity(ShipmentRequestDto dto, SalesOrder salesOrder) {
        if (dto == null) {
            return null;
        }

        return Shipment.builder()
                .salesOrder(salesOrder)
                .trackingNumber(dto.getTrackingNumber())
                .plannedDate(dto.getPlannedDate())
                .status(ShipmentStatus.PLANNED)
                .build();
    }

    public static Shipment createFromSalesOrder(SalesOrder salesOrder) {
        if (salesOrder == null) {
            return null;
        }

        return Shipment.builder()
                .salesOrder(salesOrder)
                .trackingNumber("TRK-SO-" + salesOrder.getId() + "-" + System.currentTimeMillis())
                .status(ShipmentStatus.PLANNED)
                .plannedDate(LocalDateTime.now().plusDays(3))
                .build();
    }

    public static ShipmentResponseDto toResponseDto(Shipment shipment) {
        if (shipment == null) {
            return null;
        }

        return ShipmentResponseDto.builder()
                .id(shipment.getId())
                .salesOrderId(shipment.getSalesOrder() != null ?
                        shipment.getSalesOrder().getId() : null)
                .clientName(shipment.getSalesOrder() != null ?
                        shipment.getSalesOrder().getClient().getName() : null)
                .clientEmail(shipment.getSalesOrder() != null ?
                        shipment.getSalesOrder().getClient().getEmail() : null)
                .clientPhoneNumber(shipment.getSalesOrder() != null ?
                        shipment.getSalesOrder().getClient().getPhoneNumber() : null)
                .clientAddress(shipment.getSalesOrder() != null ?
                        shipment.getSalesOrder().getClient().getAddress() : null)
                .carrierId(shipment.getCarrier() != null ?
                        shipment.getCarrier().getId() : null)
                .carrierCode(shipment.getCarrier() != null ?
                        shipment.getCarrier().getCode() : null)
                .carrierName(shipment.getCarrier() != null ?
                        shipment.getCarrier().getName() : null)
                .trackingNumber(shipment.getTrackingNumber())
                .status(shipment.getStatus())
                .plannedDate(shipment.getPlannedDate())
                .shippedDate(shipment.getShippedDate())
                .deliveredDate(shipment.getDeliveredDate())
                .createdAt(shipment.getCreatedAt())
                .build();
    }
}