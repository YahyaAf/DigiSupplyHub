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
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final ClientRepository clientRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryMovementService movementService;

    @Autowired
    public SalesOrderService(SalesOrderRepository salesOrderRepository,
                             ClientRepository clientRepository,
                             WarehouseRepository warehouseRepository,
                             ProductRepository productRepository,
                             InventoryRepository inventoryRepository,
                             InventoryMovementService movementService) {
        this.salesOrderRepository = salesOrderRepository;
        this.clientRepository = clientRepository;
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.movementService = movementService;
    }

    @Transactional
    public ApiResponse<SalesOrderResponseDto> createSalesOrder(SalesOrderRequestDto requestDto) {
        Client client = clientRepository.findById(requestDto.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", requestDto.getClientId()));

        Warehouse warehouse = warehouseRepository.findById(requestDto.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", requestDto.getWarehouseId()));

        SalesOrder salesOrder = SalesOrder.builder()
                .client(client)
                .warehouse(warehouse)
                .build();

        for (SalesOrderLineDto lineDto : requestDto.getOrderLines()) {
            Product product = productRepository.findById(lineDto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", lineDto.getProductId()));

            Integer requestedQty = lineDto.getQuantity();

            Integer availableQty = checkStockAvailability(requestDto.getWarehouseId(), product.getId(), requestedQty);

            if (availableQty < requestedQty) {
                throw new InsufficientStockException(
                        "Insufficient stock for product: " + product.getName() +
                                ". Requested: " + requestedQty + ", Available: " + availableQty
                );
            }

            SalesOrderLine line = SalesOrderMapper.toLineEntity(lineDto, salesOrder, product, false);
            salesOrder.addOrderLine(line);
        }

        SalesOrder savedOrder = salesOrderRepository.save(salesOrder);

        SalesOrderResponseDto responseDto = SalesOrderMapper.toResponseDto(savedOrder);

        return new ApiResponse<>("Sales order created successfully", responseDto);
    }

    private Integer checkStockAvailability(Long preferredWarehouseId, Long productId, Integer requestedQty) {
        Inventory preferredInventory = inventoryRepository
                .findByWarehouseIdAndProductId(preferredWarehouseId, productId)
                .orElse(null);

        if (preferredInventory != null) {
            Integer availableInPreferred = preferredInventory.getQtyOnHand() - preferredInventory.getQtyReserved();
            if (availableInPreferred >= requestedQty) {
                return availableInPreferred;
            }
        }

        Integer totalAvailable = inventoryRepository.getAvailableStockByProduct(productId);

        return totalAvailable != null ? totalAvailable : 0;
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

        for (SalesOrderLine line : salesOrder.getOrderLines()) {
            Integer requestedQty = line.getQuantity();
            Long productId = line.getProduct().getId();
            Long warehouseId = salesOrder.getWarehouse().getId();

            Inventory inventory = inventoryRepository
                    .findByWarehouseIdAndProductId(warehouseId, productId)
                    .orElseThrow(() -> new InsufficientStockException(
                            "No inventory found for product " + line.getProduct().getName() +
                                    " in warehouse " + salesOrder.getWarehouse().getName()
                    ));

            Integer available = inventory.getQtyOnHand() - inventory.getQtyReserved();

            if (available < requestedQty) {
                throw new InsufficientStockException(
                        "Insufficient stock for product: " + line.getProduct().getName() +
                                ". Requested: " + requestedQty + ", Available: " + available
                );
            }

            inventory.setQtyReserved(inventory.getQtyReserved() + requestedQty);
            inventoryRepository.save(inventory);
        }

        salesOrder.setStatus(OrderStatus.RESERVED);
        salesOrder.setReservedAt(LocalDateTime.now());

        SalesOrder savedOrder = salesOrderRepository.save(salesOrder);
        SalesOrderResponseDto responseDto = SalesOrderMapper.toResponseDto(savedOrder);

        return new ApiResponse<>("Stock reserved successfully", responseDto);
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
            Long warehouseId = salesOrder.getWarehouse().getId();

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
                            " to client " + salesOrder.getClient().getName()
            );
        }

        salesOrder.setStatus(OrderStatus.SHIPPED);
        salesOrder.setShippedAt(LocalDateTime.now());

        SalesOrder savedOrder = salesOrderRepository.save(salesOrder);
        SalesOrderResponseDto responseDto = SalesOrderMapper.toResponseDto(savedOrder);

        return new ApiResponse<>("Order shipped successfully", responseDto);
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
                                salesOrder.getWarehouse().getId(),
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