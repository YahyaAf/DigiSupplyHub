package org.project.digital_logistics.mapper;

import org.project.digital_logistics.dto.purchaseorder.*;
import org.project.digital_logistics.model.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PurchaseOrderMapper {

    private PurchaseOrderMapper() {
        throw new IllegalStateException("Utility class");
    }

    public static PurchaseOrder toEntity(PurchaseOrderRequestDto dto, Supplier supplier) {
        if (dto == null) {
            return null;
        }

        PurchaseOrder purchaseOrder = PurchaseOrder.builder()
                .supplier(supplier)
                .expectedDelivery(dto.getExpectedDelivery())
                .orderLines(new ArrayList<>())
                .build();

        // Add order lines
        if (dto.getOrderLines() != null) {
            for (PurchaseOrderLineDto lineDto : dto.getOrderLines()) {
                PurchaseOrderLine line = toLineEntity(lineDto, purchaseOrder, null);
                purchaseOrder.addOrderLine(line);
            }
        }

        return purchaseOrder;
    }

    public static PurchaseOrderLine toLineEntity(PurchaseOrderLineDto dto,
                                                 PurchaseOrder purchaseOrder,
                                                 Product product) {
        if (dto == null) {
            return null;
        }

        return PurchaseOrderLine.builder()
                .purchaseOrder(purchaseOrder)
                .product(product)
                .quantity(dto.getQuantity())
                .unitPrice(dto.getUnitPrice())
                .build();
    }

    public static PurchaseOrderResponseDto toResponseDto(PurchaseOrder purchaseOrder) {
        if (purchaseOrder == null) {
            return null;
        }

        List<PurchaseOrderLineResponseDto> lines = purchaseOrder.getOrderLines()
                .stream()
                .map(PurchaseOrderMapper::toLineResponseDto)
                .collect(Collectors.toList());

        // Calculate totals
        BigDecimal totalAmount = lines.stream()
                .map(PurchaseOrderLineResponseDto::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Integer totalItems = lines.stream()
                .map(PurchaseOrderLineResponseDto::getQuantity)
                .reduce(0, Integer::sum);

        return PurchaseOrderResponseDto.builder()
                .id(purchaseOrder.getId())
                // Supplier info
                .supplierId(purchaseOrder.getSupplier() != null ?
                        purchaseOrder.getSupplier().getId() : null)
                .supplierName(purchaseOrder.getSupplier() != null ?
                        purchaseOrder.getSupplier().getName() : null)
                .supplierContactInfo(purchaseOrder.getSupplier() != null ?
                        purchaseOrder.getSupplier().getPhoneNumber() : null)
                .supplierMarticule(purchaseOrder.getSupplier() != null ?
                        purchaseOrder.getSupplier().getMatricule() : null)
                // Status & dates
                .status(purchaseOrder.getStatus())
                .createdAt(purchaseOrder.getCreatedAt())
                .expectedDelivery(purchaseOrder.getExpectedDelivery())
                .approvedAt(purchaseOrder.getApprovedAt())
                .receivedAt(purchaseOrder.getReceivedAt())
                .canceledAt(purchaseOrder.getCanceledAt())
                // Lines
                .orderLines(lines)
                // Totals
                .totalAmount(totalAmount)
                .totalItems(totalItems)
                .build();
    }

    public static PurchaseOrderLineResponseDto toLineResponseDto(PurchaseOrderLine line) {
        if (line == null) {
            return null;
        }

        return PurchaseOrderLineResponseDto.builder()
                .id(line.getId())
                // Product info
                .productId(line.getProduct() != null ? line.getProduct().getId() : null)
                .productSku(line.getProduct() != null ? line.getProduct().getSku() : null)
                .productName(line.getProduct() != null ? line.getProduct().getName() : null)
                // Quantities
                .quantity(line.getQuantity())
                .unitPrice(line.getUnitPrice())
                .totalPrice(line.getTotalPrice())
                .build();
    }

    public static void updateEntityFromDto(PurchaseOrderRequestDto dto,
                                           PurchaseOrder purchaseOrder,
                                           Supplier supplier) {
        if (dto == null || purchaseOrder == null) {
            return;
        }

        if (supplier != null) {
            purchaseOrder.setSupplier(supplier);
        }

        if (dto.getExpectedDelivery() != null) {
            purchaseOrder.setExpectedDelivery(dto.getExpectedDelivery());
        }

        if (dto.getOrderLines() != null) {
            purchaseOrder.getOrderLines().clear();
            for (PurchaseOrderLineDto lineDto : dto.getOrderLines()) {
                PurchaseOrderLine line = toLineEntity(lineDto, purchaseOrder, null);
                purchaseOrder.addOrderLine(line);
            }
        }
    }
}