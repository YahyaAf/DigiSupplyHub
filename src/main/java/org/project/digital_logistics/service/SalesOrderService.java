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

    @Autowired
    public SalesOrderService(SalesOrderRepository salesOrderRepository,
                             ClientRepository clientRepository,
                             ProductRepository productRepository,
                             InventoryRepository inventoryRepository,
                             InventoryMovementService movementService,
                             ShipmentService shipmentService) {
        this.salesOrderRepository = salesOrderRepository;
        this.clientRepository = clientRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.movementService = movementService;
        this.shipmentService = shipmentService;
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
        Client client = clientRepository.findById(authenticatedClientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", authenticatedClientId));

        SalesOrder salesOrder = SalesOrder.builder()
                .client(client)
                .build();

        for (SalesOrderLineDto lineDto : requestDto.getOrderLines()) {
            Product product = productRepository.findById(lineDto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", lineDto.getProductId()));

            Integer requestedQty = lineDto.getQuantity();

            List<Inventory> availableInventories = inventoryRepository.findByProductId(product.getId());

            if (availableInventories.isEmpty()) {
                throw new InsufficientStockException(
                        "Product " + product.getName() + " is not available in any warehouse"
                );
            }

            Integer totalAvailable = availableInventories.stream()
                    .mapToInt(inv -> inv.getQtyOnHand() - inv.getQtyReserved())
                    .sum();

            if (totalAvailable < requestedQty) {
                throw new InsufficientStockException(
                        "Insufficient stock for product: " + product.getName() +
                                ". Requested: " + requestedQty + ", Available: " + totalAvailable
                );
            }

            Warehouse tempWarehouse = availableInventories.stream()
                    .filter(inv -> (inv.getQtyOnHand() - inv.getQtyReserved()) > 0)
                    .findFirst()
                    .map(Inventory::getWarehouse)
                    .orElseThrow(() -> new InsufficientStockException("No available stock"));

            SalesOrderLine line = SalesOrderMapper.toLineEntity(
                    lineDto, salesOrder, product, tempWarehouse, requestedQty, false
            );
            salesOrder.addOrderLine(line);
        }

        // 4. Save
        SalesOrder savedOrder = salesOrderRepository.save(salesOrder);
        SalesOrderResponseDto responseDto = SalesOrderMapper.toResponseDto(savedOrder);

        return new ApiResponse<>("Sales order created successfully", responseDto);
    }

    @Transactional
    public ApiResponse<SalesOrderResponseDto> reserveStock(Long id) {
        SalesOrder salesOrder = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", "id", id));

        if (salesOrder.getStatus() != OrderStatus.CREATED) {
            throw new InvalidOperationException(
                    "Can only reserve stock for CREATED orders. Current status: " + salesOrder.getStatus()
            );
        }

        List<SalesOrderLine> originalLines = new ArrayList<>(salesOrder.getOrderLines());
        salesOrder.getOrderLines().clear();

        for (SalesOrderLine originalLine : originalLines) {
            Product product = originalLine.getProduct();
            Integer requestedQty = originalLine.getQuantity();

            List<StockAllocation> allocations = allocateStockAcrossWarehouses(product.getId(), requestedQty);

            if (allocations.isEmpty()) {
                throw new InsufficientStockException(
                        "Cannot reserve stock for product: " + product.getName()
                );
            }

            for (StockAllocation allocation : allocations) {
                Inventory inventory = inventoryRepository
                        .findByWarehouseIdAndProductId(allocation.warehouse.getId(), product.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

                inventory.setQtyReserved(inventory.getQtyReserved() + allocation.quantity);
                inventoryRepository.save(inventory);

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

        salesOrder.setStatus(OrderStatus.RESERVED);
        salesOrder.setReservedAt(LocalDateTime.now());

        SalesOrder savedOrder = salesOrderRepository.save(salesOrder);
        SalesOrderResponseDto responseDto = SalesOrderMapper.toResponseDto(savedOrder);

        return new ApiResponse<>("Stock reserved successfully from multiple warehouses", responseDto);
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