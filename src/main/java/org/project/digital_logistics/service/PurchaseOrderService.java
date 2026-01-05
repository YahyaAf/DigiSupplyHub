package org.project.digital_logistics.service;

import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.purchaseorder.PurchaseOrderLineDto;
import org.project.digital_logistics.dto.purchaseorder.PurchaseOrderRequestDto;
import org.project.digital_logistics.dto.purchaseorder.PurchaseOrderResponseDto;
import org.project.digital_logistics.enums.PurchaseOrderStatus;
import org.project.digital_logistics.exception.InvalidOperationException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.mapper.PurchaseOrderMapper;
import org.project.digital_logistics.model.*;
import org.project.digital_logistics.model.enums.MovementType;
import org.project.digital_logistics.model.enums.OrderStatus;
import org.project.digital_logistics.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryMovementService movementService;
    private final SalesOrderRepository salesOrderRepository;

    @Autowired
    public PurchaseOrderService(PurchaseOrderRepository purchaseOrderRepository,
                                SupplierRepository supplierRepository,
                                ProductRepository productRepository,
                                InventoryRepository inventoryRepository,
                                WarehouseRepository warehouseRepository,
                                InventoryMovementService movementService,
                                SalesOrderRepository salesOrderRepository) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.warehouseRepository = warehouseRepository;
        this.movementService = movementService;
        this.salesOrderRepository = salesOrderRepository;
    }

    @Transactional
    public ApiResponse<PurchaseOrderResponseDto> createPurchaseOrder(PurchaseOrderRequestDto requestDto) {
        Supplier supplier = supplierRepository.findById(requestDto.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", requestDto.getSupplierId()));

        PurchaseOrder purchaseOrder = PurchaseOrder.builder()
                .supplier(supplier)
                .expectedDelivery(requestDto.getExpectedDelivery())
                .build();

        for (PurchaseOrderLineDto lineDto : requestDto.getOrderLines()) {
            Product product = productRepository.findById(lineDto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", lineDto.getProductId()));

            PurchaseOrderLine line = PurchaseOrderMapper.toLineEntity(lineDto, purchaseOrder, product);
            purchaseOrder.addOrderLine(line);
        }

        PurchaseOrder savedOrder = purchaseOrderRepository.save(purchaseOrder);

        PurchaseOrderResponseDto responseDto = PurchaseOrderMapper.toResponseDto(savedOrder);

        return new ApiResponse<>("Purchase order created successfully", responseDto);
    }

    public ApiResponse<PurchaseOrderResponseDto> getPurchaseOrderById(Long id) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));

        PurchaseOrderResponseDto responseDto = PurchaseOrderMapper.toResponseDto(purchaseOrder);
        return new ApiResponse<>("Purchase order retrieved successfully", responseDto);
    }

    public ApiResponse<List<PurchaseOrderResponseDto>> getAllPurchaseOrders() {
        List<PurchaseOrderResponseDto> orders = purchaseOrderRepository.findAll()
                .stream()
                .map(PurchaseOrderMapper::toResponseDto)
                .toList();
        return new ApiResponse<>("Purchase orders retrieved successfully", orders);
    }

    public ApiResponse<List<PurchaseOrderResponseDto>> getPurchaseOrdersByStatus(PurchaseOrderStatus status) {
        List<PurchaseOrderResponseDto> orders = purchaseOrderRepository.findByStatus(status)
                .stream()
                .map(PurchaseOrderMapper::toResponseDto)
                .toList();
        return new ApiResponse<>("Purchase orders retrieved successfully", orders);
    }

    public ApiResponse<List<PurchaseOrderResponseDto>> getPurchaseOrdersBySupplier(Long supplierId) {
        if (!supplierRepository.existsById(supplierId)) {
            throw new ResourceNotFoundException("Supplier", "id", supplierId);
        }

        List<PurchaseOrderResponseDto> orders = purchaseOrderRepository.findBySupplierId(supplierId)
                .stream()
                .map(PurchaseOrderMapper::toResponseDto)
                .toList();
        return new ApiResponse<>("Supplier purchase orders retrieved successfully", orders);
    }

    @Transactional
    public ApiResponse<PurchaseOrderResponseDto> updatePurchaseOrder(Long id, PurchaseOrderRequestDto requestDto) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));

        if (purchaseOrder.getStatus() == PurchaseOrderStatus.RECEIVED ||
                purchaseOrder.getStatus() == PurchaseOrderStatus.CANCELED) {
            throw new InvalidOperationException(
                    "Cannot update purchase order with status: " + purchaseOrder.getStatus()
            );
        }

        Supplier supplier = supplierRepository.findById(requestDto.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", requestDto.getSupplierId()));

        purchaseOrder.getOrderLines().clear();

        for (PurchaseOrderLineDto lineDto : requestDto.getOrderLines()) {
            Product product = productRepository.findById(lineDto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", lineDto.getProductId()));

            PurchaseOrderLine line = PurchaseOrderMapper.toLineEntity(lineDto, purchaseOrder, product);
            purchaseOrder.addOrderLine(line);
        }

        purchaseOrder.setSupplier(supplier);
        purchaseOrder.setExpectedDelivery(requestDto.getExpectedDelivery());

        PurchaseOrder savedOrder = purchaseOrderRepository.save(purchaseOrder);

        PurchaseOrderResponseDto responseDto = PurchaseOrderMapper.toResponseDto(savedOrder);

        return new ApiResponse<>("Purchase order updated successfully", responseDto);
    }

    //  APPROVE Purchase Order
    @Transactional
    public ApiResponse<PurchaseOrderResponseDto> approvePurchaseOrder(Long id) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));

        // Validate current status
        if (purchaseOrder.getStatus() != PurchaseOrderStatus.CREATED) {
            throw new InvalidOperationException(
                    "Can only approve purchase orders with CREATED status. Current status: " + purchaseOrder.getStatus()
            );
        }

        // Update status
        purchaseOrder.setStatus(PurchaseOrderStatus.APPROVED);
        purchaseOrder.setApprovedAt(LocalDateTime.now());

        PurchaseOrder savedOrder = purchaseOrderRepository.save(purchaseOrder);
        PurchaseOrderResponseDto responseDto = PurchaseOrderMapper.toResponseDto(savedOrder);

        return new ApiResponse<>("Purchase order approved successfully", responseDto);
    }

    //  RECEIVE Purchase Order (+ Update Inventory)
    @Transactional
    public ApiResponse<PurchaseOrderResponseDto> receivePurchaseOrder(Long id, Long warehouseId) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));

        if (purchaseOrder.getStatus() != PurchaseOrderStatus.APPROVED) {
            throw new InvalidOperationException(
                    "Can only receive purchase orders with APPROVED status. Current status: " + purchaseOrder.getStatus()
            );
        }

        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", warehouseId));

        for (PurchaseOrderLine line : purchaseOrder.getOrderLines()) {
            Product product = line.getProduct();
            Integer quantity = line.getQuantity();

            // Find or create inventory
            Inventory inventory = inventoryRepository
                    .findByWarehouseIdAndProductId(warehouseId, product.getId())
                    .orElse(Inventory.builder()
                            .warehouse(warehouse)
                            .product(product)
                            .qtyOnHand(0)
                            .qtyReserved(0)
                            .build());

            // Add received quantity
            inventory.setQtyOnHand(inventory.getQtyOnHand() + quantity);
            Inventory savedInventory = inventoryRepository.save(inventory);

            movementService.recordMovement(
                    savedInventory.getId(),
                    MovementType.INBOUND,
                    quantity,
                    "PO-" + id,
                    "Purchase order reception - " + product.getName()
            );
        }

        purchaseOrder.setStatus(PurchaseOrderStatus.RECEIVED);
        purchaseOrder.setReceivedAt(LocalDateTime.now());

        PurchaseOrder savedOrder = purchaseOrderRepository.save(purchaseOrder);

        // Check if this PO is related to a Sales Order and try to auto-reserve
        String autoReserveMessage = "";
        if (savedOrder.getRelatedSalesOrderId() != null) {
            autoReserveMessage = tryAutoReserveSalesOrder(savedOrder.getRelatedSalesOrderId());
        }

        PurchaseOrderResponseDto responseDto = PurchaseOrderMapper.toResponseDto(savedOrder);

        String finalMessage = "Purchase order received successfully and inventory updated";
        if (!autoReserveMessage.isEmpty()) {
            finalMessage += "\n\n" + autoReserveMessage;
        }

        return new ApiResponse<>(finalMessage, responseDto);
    }

    /**
     * Try to automatically reserve a Sales Order if all required stock is now available
     */
    private String tryAutoReserveSalesOrder(Long salesOrderId) {
        try {
            SalesOrder salesOrder = salesOrderRepository.findById(salesOrderId).orElse(null);

            if (salesOrder == null || salesOrder.getStatus() != OrderStatus.BACKORDER) {
                return "";
            }

            // Check if ALL products now have enough stock
            boolean allStockAvailable = true;
            for (SalesOrderLine line : salesOrder.getOrderLines()) {
                Product product = line.getProduct();
                Integer requestedQty = line.getQuantity();

                Integer totalAvailable = inventoryRepository.findByProductId(product.getId())
                        .stream()
                        .mapToInt(inv -> inv.getQtyOnHand() - inv.getQtyReserved())
                        .sum();

                if (totalAvailable < requestedQty) {
                    allStockAvailable = false;
                    break;
                }
            }

            if (allStockAvailable) {
                // All stock is now available! Reserve the stock automatically
                boolean reservationSuccess = performStockReservation(salesOrder);

                if (reservationSuccess) {
                    return "Bonne nouvelle! Tous les produits de la commande #" + salesOrderId +
                           " sont maintenant en stock et ont été réservés automatiquement. Status: RESERVED";
                } else {
                    // If reservation fails, change status back to CREATED so client can try
                    salesOrder.setStatus(OrderStatus.CREATED);
                    salesOrderRepository.save(salesOrder);
                    return "Tous les produits de la commande #" + salesOrderId +
                           " sont maintenant en stock. Status changé à CREATED. Le client peut maintenant réserver.";
                }
            } else {
                return "Stock reçu, mais d'autres produits sont encore en attente pour la commande #" + salesOrderId;
            }

        } catch (Exception e) {
            System.err.println("Error trying to auto-reserve Sales Order " + salesOrderId + ": " + e.getMessage());
            return "";
        }
    }

    /**
     * Perform actual stock reservation for a Sales Order
     * Updates qtyReserved in inventories and changes status to RESERVED
     */
    private boolean performStockReservation(SalesOrder salesOrder) {
        try {
            // For each product in the sales order, reserve stock from available inventories
            for (SalesOrderLine line : salesOrder.getOrderLines()) {
                Product product = line.getProduct();
                Integer requestedQty = line.getQuantity();
                Integer remainingQty = requestedQty;

                // Get all inventories for this product sorted by available quantity (descending)
                List<Inventory> inventories = inventoryRepository.findByProductId(product.getId())
                        .stream()
                        .filter(inv -> (inv.getQtyOnHand() - inv.getQtyReserved()) > 0)
                        .sorted((a, b) -> Integer.compare(
                                b.getQtyOnHand() - b.getQtyReserved(),
                                a.getQtyOnHand() - a.getQtyReserved()
                        ))
                        .toList();

                // Reserve stock from each warehouse
                for (Inventory inventory : inventories) {
                    if (remainingQty <= 0) break;

                    Integer available = inventory.getQtyOnHand() - inventory.getQtyReserved();
                    Integer toReserve = Math.min(available, remainingQty);

                    // Update reserved quantity
                    inventory.setQtyReserved(inventory.getQtyReserved() + toReserve);
                    inventoryRepository.save(inventory);

                    remainingQty -= toReserve;
                }

                // Check if all quantity was reserved
                if (remainingQty > 0) {
                    // Rollback reservations and return false
                    return false;
                }
            }

            // All stock reserved successfully, update status
            salesOrder.setStatus(OrderStatus.RESERVED);
            salesOrder.setReservedAt(LocalDateTime.now());
            salesOrderRepository.save(salesOrder);

            return true;
        } catch (Exception e) {
            System.err.println("Error performing stock reservation: " + e.getMessage());
            return false;
        }
    }

    @Transactional
    public ApiResponse<PurchaseOrderResponseDto> cancelPurchaseOrder(Long id) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));

        if (purchaseOrder.getStatus() == PurchaseOrderStatus.RECEIVED) {
            throw new InvalidOperationException(
                    "Cannot cancel a purchase order that has already been received"
            );
        }

        if (purchaseOrder.getStatus() == PurchaseOrderStatus.CANCELED) {
            throw new InvalidOperationException("Purchase order is already canceled");
        }

        purchaseOrder.setStatus(PurchaseOrderStatus.CANCELED);
        purchaseOrder.setCanceledAt(LocalDateTime.now());

        PurchaseOrder savedOrder = purchaseOrderRepository.save(purchaseOrder);
        PurchaseOrderResponseDto responseDto = PurchaseOrderMapper.toResponseDto(savedOrder);

        return new ApiResponse<>("Purchase order canceled successfully", responseDto);
    }

    @Transactional
    public ApiResponse<Void> deletePurchaseOrder(Long id) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));

        // Only allow deletion if CREATED or CANCELED
        if (purchaseOrder.getStatus() == PurchaseOrderStatus.APPROVED ||
                purchaseOrder.getStatus() == PurchaseOrderStatus.RECEIVED) {
            throw new InvalidOperationException(
                    "Cannot delete purchase order with status: " + purchaseOrder.getStatus()
            );
        }

        purchaseOrderRepository.deleteById(id);
        return new ApiResponse<>("Purchase order deleted successfully", null);
    }

    public ApiResponse<Long> countPurchaseOrders() {
        long count = purchaseOrderRepository.count();
        return new ApiResponse<>("Total purchase orders counted successfully", count);
    }

    public ApiResponse<Long> countPurchaseOrdersByStatus(PurchaseOrderStatus status) {
        long count = purchaseOrderRepository.countByStatus(status);
        return new ApiResponse<>("Purchase orders counted successfully", count);
    }

    /**
     * Create a Purchase Order automatically for out-of-stock products
     * This is triggered when a Sales Order cannot be reserved due to insufficient stock
     */
    @Transactional
    public PurchaseOrder createAutoPurchaseOrder(Product product, Integer quantity, Long salesOrderId) {
        // Find a supplier for this product (use the first available supplier)
        // In a real scenario, you might want to select based on product-supplier relationships
        Supplier supplier = supplierRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No supplier available to create auto purchase order"));

        PurchaseOrder purchaseOrder = PurchaseOrder.builder()
                .supplier(supplier)
                .expectedDelivery(LocalDateTime.now().plusDays(7)) // Default 7 days delivery
                .relatedSalesOrderId(salesOrderId)
                .build();

        PurchaseOrderLine line = PurchaseOrderLine.builder()
                .purchaseOrder(purchaseOrder)
                .product(product)
                .quantity(quantity)
                .unitPrice(product.getOriginalPrice() != null ? BigDecimal.valueOf(product.getOriginalPrice()) : BigDecimal.ZERO)
                .build();

        purchaseOrder.addOrderLine(line);

        return purchaseOrderRepository.save(purchaseOrder);
    }
}