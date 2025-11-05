package org.project.digital_logistics.mapper;

import org.project.digital_logistics.dto.salesorder.*;
import org.project.digital_logistics.model.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class SalesOrderMapper {

    private SalesOrderMapper() {
        throw new IllegalStateException("Utility class");
    }

    public static SalesOrder toEntity(SalesOrderRequestDto dto, Client client) {
        if (dto == null) {
            return null;
        }

        return SalesOrder.builder()
                .client(client)
                .build();
    }

    public static SalesOrderLine toLineEntity(SalesOrderLineDto dto,
                                              SalesOrder salesOrder,
                                              Product product,
                                              Warehouse warehouse,
                                              Integer quantity,
                                              Boolean backOrder) {
        if (dto == null) {
            return null;
        }

        return SalesOrderLine.builder()
                .salesOrder(salesOrder)
                .product(product)
                .warehouse(warehouse)
                .quantity(quantity)
                .unitPrice(dto.getUnitPrice())
                .backOrder(backOrder != null ? backOrder : false)
                .build();
    }

    public static SalesOrderResponseDto toResponseDto(SalesOrder salesOrder) {
        if (salesOrder == null) {
            return null;
        }

        List<SalesOrderLineResponseDto> lines = salesOrder.getOrderLines()
                .stream()
                .map(SalesOrderMapper::toLineResponseDto)
                .collect(Collectors.toList());

        BigDecimal totalAmount = lines.stream()
                .map(SalesOrderLineResponseDto::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Integer totalItems = lines.stream()
                .map(SalesOrderLineResponseDto::getQuantity)
                .reduce(0, Integer::sum);

        return SalesOrderResponseDto.builder()
                .id(salesOrder.getId())
                .clientId(salesOrder.getClient() != null ?
                        salesOrder.getClient().getId() : null)
                .clientName(salesOrder.getClient() != null ?
                        salesOrder.getClient().getName() : null)
                .clientEmail(salesOrder.getClient() != null ?
                        salesOrder.getClient().getEmail() : null)
                .clientPhoneNumber(salesOrder.getClient() != null ?
                        salesOrder.getClient().getPhoneNumber() : null)
                .clientAddress(salesOrder.getClient() != null ?
                        salesOrder.getClient().getAddress() : null)
                .status(salesOrder.getStatus())
                .createdAt(salesOrder.getCreatedAt())
                .reservedAt(salesOrder.getReservedAt())
                .shippedAt(salesOrder.getShippedAt())
                .deliveredAt(salesOrder.getDeliveredAt())
                .orderLines(lines)
                .totalAmount(totalAmount)
                .totalItems(totalItems)
                .build();
    }

    public static SalesOrderLineResponseDto toLineResponseDto(SalesOrderLine line) {
        if (line == null) {
            return null;
        }

        return SalesOrderLineResponseDto.builder()
                .id(line.getId())
                .productId(line.getProduct() != null ? line.getProduct().getId() : null)
                .productSku(line.getProduct() != null ? line.getProduct().getSku() : null)
                .productName(line.getProduct() != null ? line.getProduct().getName() : null)
                .warehouseId(line.getWarehouse() != null ? line.getWarehouse().getId() : null)
                .warehouseCode(line.getWarehouse() != null ? line.getWarehouse().getCode() : null)
                .warehouseName(line.getWarehouse() != null ? line.getWarehouse().getName() : null)
                .quantity(line.getQuantity())
                .unitPrice(line.getUnitPrice())
                .totalPrice(line.getTotalPrice())
                .backOrder(line.getBackOrder())
                .build();
    }
}