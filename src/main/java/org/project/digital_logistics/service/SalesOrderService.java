package org.project.digital_logistics.service;

import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.salesorder.SalesOrderLineDto;
import org.project.digital_logistics.dto.salesorder.SalesOrderRequestDto;
import org.project.digital_logistics.dto.salesorder.SalesOrderResponseDto;
import org.project.digital_logistics.model.enums.MovementType;
import org.project.digital_logistics.model.enums.OrderStatus;
import org.project.digital_logistics.exception.InsufficientStockException;
import org.project.digital_logistics.exception.InvalidOperationException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.mapper.SalesOrderMapper;
import org.project.digital_logistics.model.*;
import org.project.digital_logistics.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryMovementService movementService;
    private final ShipmentService shipmentService;
    private final PurchaseOrderService purchaseOrderService;

    @Autowired
    public SalesOrderService(SalesOrderRepository salesOrderRepository,
                             ClientRepository clientRepository,
                             ProductRepository productRepository,
                             InventoryRepository inventoryRepository,
                             InventoryMovementService movementService,
                             ShipmentService shipmentService,
                             PurchaseOrderService purchaseOrderService) {
        this.salesOrderRepository = salesOrderRepository;
        this.clientRepository = clientRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.movementService = movementService;
        this.shipmentService = shipmentService;
        this.purchaseOrderService = purchaseOrderService;
    }

    private static class StockAllocation {
        Warehouse warehouse;
        Integer quantity;

        StockAllocation(Warehouse warehouse, Integer quantity) {
            this.warehouse = warehouse;
            this.quantity = quantity;
        }
    }

    @Transactional
    public ApiResponse<SalesOrderResponseDto> createSalesOrder(SalesOrderRequestDto requestDto, Long authenticatedClientId) {
        // 1. Fetch Client
        Client client = clientRepository.findById(authenticatedClientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", authenticatedClientId));

        // 2. Create Sales Order with CREATED status
        SalesOrder salesOrder = SalesOrder.builder()
                .client(client)
                .status(OrderStatus.CREATED)
                .build();

        // 3. Create order lines (temporary warehouse assignment)
        for (SalesOrderLineDto lineDto : requestDto.getOrderLines()) {
            Product product = productRepository.findById(lineDto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", lineDto.getProductId()));

            Integer requestedQty = lineDto.getQuantity();

            // Get first available warehouse (temporary)
            Warehouse tempWarehouse = inventoryRepository.findByProductId(product.getId())
                    .stream()
                    .filter(inv -> (inv.getQtyOnHand() - inv.getQtyReserved()) > 0)
                    .findFirst()
                    .map(Inventory::getWarehouse)
                    .orElse(null);

            if (tempWarehouse == null) {
                // Use any warehouse for now (will be properly allocated on reserve)
                tempWarehouse = inventoryRepository.findByProductId(product.getId())
                        .stream()
                        .findFirst()
                        .map(Inventory::getWarehouse)
                        .orElse(null);

                // If still no warehouse, use a default one or skip
                if (tempWarehouse == null) {
                    throw new ResourceNotFoundException(
                            "No warehouse found for product: " + product.getName()
                    );
                }
            }

            SalesOrderLine line = SalesOrderMapper.toLineEntity(
                    lineDto, salesOrder, product, tempWarehouse, requestedQty, false
            );
            salesOrder.addOrderLine(line);
        }

        // 4. Save order with CREATED status
        SalesOrder savedOrder = salesOrderRepository.save(salesOrder);

        String message = "Sales order created successfully with status CREATED. " +
                "To reserve stock, call the reserve endpoint.";

        SalesOrderResponseDto responseDto = SalesOrderMapper.toResponseDto(savedOrder);
        return new ApiResponse<>(message, responseDto);
    }

    @Transactional
    protected SalesOrder performStockReservation(SalesOrder salesOrder) {
        // Clear existing lines
        List<SalesOrderLine> originalLines = new ArrayList<>(salesOrder.getOrderLines());
        salesOrder.getOrderLines().clear();

        // Allocate stock from warehouses
        for (SalesOrderLine originalLine : originalLines) {
            Product product = originalLine.getProduct();
            Integer requestedQty = originalLine.getQuantity();

            List<StockAllocation> allocations = allocateStockAcrossWarehouses(product.getId(), requestedQty);

            if (allocations.isEmpty()) {
                throw new InsufficientStockException(
                        "Cannot reserve stock for product: " + product.getName()
                );
            }

            // Reserve stock and create lines
            for (StockAllocation allocation : allocations) {
                Inventory inventory = inventoryRepository
                        .findByWarehouseIdAndProductId(allocation.warehouse.getId(), product.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

                // Reserve stock
                inventory.setQtyReserved(inventory.getQtyReserved() + allocation.quantity);
                inventoryRepository.save(inventory);

                // Create line
                SalesOrderLine line = SalesOrderMapper.toLineEntity(
                        SalesOrderLineDto.builder()
                                .productId(product.getId())
                                .quantity(allocation.quantity)
                                .unitPrice(originalLine.getUnitPrice())
                                .build(),
                        salesOrder,
                        product,
                        allocation.warehouse,
                        allocation.quantity,
                        false
                );
                salesOrder.addOrderLine(line);
            }
        }

        // Update status
        salesOrder.setStatus(OrderStatus.RESERVED);
        salesOrder.setReservedAt(LocalDateTime.now());

        return salesOrderRepository.save(salesOrder);
    }

    @Transactional
    public ApiResponse<SalesOrderResponseDto> reserveStock(Long id) {
        SalesOrder salesOrder = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", "id", id));

        if (salesOrder.getStatus() != OrderStatus.CREATED) {
            if (salesOrder.getStatus() == OrderStatus.BACKORDER) {
                throw new InvalidOperationException(
                        "Impossible de réserver cette commande. " +
                        "Elle est en attente de réapprovisionnement (status: BACKORDER). " +
                        "Veuillez patienter que tous les produits soient disponibles. " +
                        "Vous serez notifié quand la réservation sera possible."
                );
            }
            throw new InvalidOperationException(
                    "Can only reserve stock for CREATED orders. Current status: " + salesOrder.getStatus()
            );
        }

        // Check stock availability for each product
        Map<Long, Integer> insufficientProducts = new HashMap<>(); // productId -> missing quantity
        List<PurchaseOrder> createdPurchaseOrders = new ArrayList<>();
        boolean allStockAvailable = true;

        for (SalesOrderLine line : salesOrder.getOrderLines()) {
            Product product = line.getProduct();
            Integer requestedQty = line.getQuantity();

            Integer totalAvailable = inventoryRepository.findByProductId(product.getId())
                    .stream()
                    .mapToInt(inv -> inv.getQtyOnHand() - inv.getQtyReserved())
                    .sum();

            if (totalAvailable < requestedQty) {
                Integer missingQty = requestedQty - totalAvailable;
                insufficientProducts.put(product.getId(), missingQty);
                allStockAvailable = false;

                // Create automatic Purchase Order for the missing quantity
                try {
                    PurchaseOrder autoPO = purchaseOrderService.createAutoPurchaseOrder(
                            product, missingQty, salesOrder.getId()
                    );
                    createdPurchaseOrders.add(autoPO);
                } catch (Exception e) {
                    // Log error but continue processing
                    System.err.println("Failed to create auto purchase order for product: " + product.getName());
                }
            }
        }

        // If some products have insufficient stock
        if (!allStockAvailable) {
            // Change status to BACKORDER
            salesOrder.setStatus(OrderStatus.BACKORDER);
            salesOrderRepository.save(salesOrder);

            StringBuilder message = new StringBuilder();
            message.append("Stock insuffisant pour certains produits:\n\n");

            for (Map.Entry<Long, Integer> entry : insufficientProducts.entrySet()) {
                Product product = productRepository.findById(entry.getKey()).orElse(null);
                if (product != null) {
                    message.append("• ").append(product.getName())
                           .append(" - Quantité manquante: ").append(entry.getValue()).append(" unités\n");
                }
            }

            message.append("\nBonne nouvelle! Nous avons automatiquement créé ")
                   .append(createdPurchaseOrders.size())
                   .append(" commande(s) d'achat pour réapprovisionner le stock.\n\n");
            message.append("Votre commande sera automatiquement réservée dès réception de TOUS les produits manquants.\n\n");
            message.append("Statut de votre commande: BACKORDER (en attente de réapprovisionnement)");

            SalesOrderResponseDto responseDto = SalesOrderMapper.toResponseDto(salesOrder);
            return new ApiResponse<>(message.toString(), responseDto);
        }

        // All stock is available, proceed with reservation
        try {
            SalesOrder reservedOrder = performStockReservation(salesOrder);
            SalesOrderResponseDto responseDto = SalesOrderMapper.toResponseDto(reservedOrder);

            return new ApiResponse<>(
                    "Stock réservé avec succès! Votre commande est prête à être expédiée.",
                    responseDto
            );
        } catch (Exception e) {
            throw new InsufficientStockException(
                    "Impossible de réserver le stock. Une erreur est survenue: " + e.getMessage()
            );
        }
    }

    private List<StockAllocation> allocateStockAcrossWarehouses(Long productId, Integer requestedQty) {
        List<StockAllocation> allocations = new ArrayList<>();
        Integer remaining = requestedQty;

        List<Inventory> inventories = inventoryRepository.findByProductId(productId)
                .stream()
                .filter(inv -> (inv.getQtyOnHand() - inv.getQtyReserved()) > 0)
                .sorted((a, b) -> Integer.compare(
                        b.getQtyOnHand() - b.getQtyReserved(),
                        a.getQtyOnHand() - a.getQtyReserved()
                ))
                .toList();

        for (Inventory inventory : inventories) {
            if (remaining <= 0) break;

            Integer available = inventory.getQtyOnHand() - inventory.getQtyReserved();
            Integer toAllocate = Math.min(available, remaining);

            allocations.add(new StockAllocation(inventory.getWarehouse(), toAllocate));
            remaining -= toAllocate;
        }

        if (remaining > 0) {
            return Collections.emptyList();
        }

        return allocations;
    }

    @Transactional
    public ApiResponse<SalesOrderResponseDto> shipOrder(Long id) {
        SalesOrder salesOrder = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", "id", id));

        if (salesOrder.getStatus() != OrderStatus.RESERVED) {
            throw new InvalidOperationException(
                    "Can only ship RESERVED orders. Current status: " + salesOrder.getStatus()
            );
        }

        for (SalesOrderLine line : salesOrder.getOrderLines()) {
            Integer shippedQty = line.getQuantity();
            Long productId = line.getProduct().getId();
            Long warehouseId = line.getWarehouse().getId();

            Inventory inventory = inventoryRepository
                    .findByWarehouseIdAndProductId(warehouseId, productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

            inventory.setQtyOnHand(inventory.getQtyOnHand() - shippedQty);
            inventory.setQtyReserved(inventory.getQtyReserved() - shippedQty);
            Inventory savedInventory = inventoryRepository.save(inventory);

            movementService.recordMovement(
                    savedInventory.getId(),
                    MovementType.OUTBOUND,
                    shippedQty,
                    "SO-" + id,
                    "Sales order shipment - " + line.getProduct().getName() +
                            " from warehouse " + line.getWarehouse().getName() +
                            " to client " + salesOrder.getClient().getName()
            );
        }

        salesOrder.setStatus(OrderStatus.SHIPPED);
        salesOrder.setShippedAt(LocalDateTime.now());

        SalesOrder savedOrder = salesOrderRepository.save(salesOrder);

        Shipment shipment = shipmentService.autoCreateShipment(savedOrder);

        SalesOrderResponseDto responseDto = SalesOrderMapper.toResponseDto(savedOrder);

        return new ApiResponse<>(
                "Order shipped successfully. Tracking number: " + shipment.getTrackingNumber(),
                responseDto
        );
    }

    @Transactional
    public ApiResponse<SalesOrderResponseDto> deliverOrder(Long id) {
        SalesOrder salesOrder = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", "id", id));

        if (salesOrder.getStatus() != OrderStatus.SHIPPED) {
            throw new InvalidOperationException(
                    "Can only deliver SHIPPED orders. Current status: " + salesOrder.getStatus()
            );
        }

        salesOrder.setStatus(OrderStatus.DELIVERED);
        salesOrder.setDeliveredAt(LocalDateTime.now());

        SalesOrder savedOrder = salesOrderRepository.save(salesOrder);
        SalesOrderResponseDto responseDto = SalesOrderMapper.toResponseDto(savedOrder);

        return new ApiResponse<>("Order delivered successfully", responseDto);
    }

    @Transactional
    public ApiResponse<SalesOrderResponseDto> cancelOrder(Long id) {
        SalesOrder salesOrder = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", "id", id));

        if (salesOrder.getStatus() == OrderStatus.SHIPPED ||
                salesOrder.getStatus() == OrderStatus.DELIVERED) {
            throw new InvalidOperationException(
                    "Cannot cancel order with status: " + salesOrder.getStatus()
            );
        }

        if (salesOrder.getStatus() == OrderStatus.RESERVED) {
            for (SalesOrderLine line : salesOrder.getOrderLines()) {
                Inventory inventory = inventoryRepository
                        .findByWarehouseIdAndProductId(
                                line.getWarehouse().getId(),
                                line.getProduct().getId()
                        )
                        .orElse(null);

                if (inventory != null) {
                    inventory.setQtyReserved(inventory.getQtyReserved() - line.getQuantity());
                    inventoryRepository.save(inventory);
                }
            }
        }

        salesOrder.setStatus(OrderStatus.CANCELED);

        SalesOrder savedOrder = salesOrderRepository.save(salesOrder);
        SalesOrderResponseDto responseDto = SalesOrderMapper.toResponseDto(savedOrder);

        return new ApiResponse<>("Order canceled successfully", responseDto);
    }

    public ApiResponse<SalesOrderResponseDto> getSalesOrderById(Long id) {
        SalesOrder salesOrder = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", "id", id));

        SalesOrderResponseDto responseDto = SalesOrderMapper.toResponseDto(salesOrder);
        return new ApiResponse<>("Sales order retrieved successfully", responseDto);
    }

    public ApiResponse<List<SalesOrderResponseDto>> getAllSalesOrders() {
        List<SalesOrderResponseDto> orders = salesOrderRepository.findAll()
                .stream()
                .map(SalesOrderMapper::toResponseDto)
                .toList();
        return new ApiResponse<>("Sales orders retrieved successfully", orders);
    }

    public ApiResponse<List<SalesOrderResponseDto>> getSalesOrdersByStatus(OrderStatus status) {
        List<SalesOrderResponseDto> orders = salesOrderRepository.findByStatus(status)
                .stream()
                .map(SalesOrderMapper::toResponseDto)
                .toList();
        return new ApiResponse<>("Sales orders retrieved successfully", orders);
    }

    public ApiResponse<List<SalesOrderResponseDto>> getSalesOrdersByClient(Long clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException("Client", "id", clientId);
        }

        List<SalesOrderResponseDto> orders = salesOrderRepository.findByClientId(clientId)
                .stream()
                .map(SalesOrderMapper::toResponseDto)
                .toList();
        return new ApiResponse<>("Client sales orders retrieved successfully", orders);
    }

    public ApiResponse<Long> countSalesOrders() {
        long count = salesOrderRepository.count();
        return new ApiResponse<>("Total sales orders counted successfully", count);
    }
}