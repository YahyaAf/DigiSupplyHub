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
import org.project.digital_logistics.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    public PurchaseOrderService(PurchaseOrderRepository purchaseOrderRepository,
                                SupplierRepository supplierRepository,
                                ProductRepository productRepository,
                                InventoryRepository inventoryRepository,
                                WarehouseRepository warehouseRepository) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.warehouseRepository = warehouseRepository;
    }

    @Transactional
    public ApiResponse<PurchaseOrderResponseDto> createPurchaseOrder(PurchaseOrderRequestDto requestDto) {
        // 1. Fetch Supplier
        Supplier supplier = supplierRepository.findById(requestDto.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", requestDto.getSupplierId()));

        // 2. Validate and fetch Products for each line
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

        // 3. Save
        PurchaseOrder savedOrder = purchaseOrderRepository.save(purchaseOrder);

        // 4. Convert to DTO
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
        // Verify supplier exists
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
        // 1. Find purchase order
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));

        // 2. Check if can be updated
        if (purchaseOrder.getStatus() == PurchaseOrderStatus.RECEIVED ||
                purchaseOrder.getStatus() == PurchaseOrderStatus.CANCELED) {
            throw new InvalidOperationException(
                    "Cannot update purchase order with status: " + purchaseOrder.getStatus()
            );
        }

        // 3. Fetch Supplier
        Supplier supplier = supplierRepository.findById(requestDto.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", requestDto.getSupplierId()));

        // 4. Clear old lines and add new ones
        purchaseOrder.getOrderLines().clear();

        for (PurchaseOrderLineDto lineDto : requestDto.getOrderLines()) {
            Product product = productRepository.findById(lineDto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", lineDto.getProductId()));

            PurchaseOrderLine line = PurchaseOrderMapper.toLineEntity(lineDto, purchaseOrder, product);
            purchaseOrder.addOrderLine(line);
        }

        purchaseOrder.setSupplier(supplier);
        purchaseOrder.setExpectedDelivery(requestDto.getExpectedDelivery());

        // 5. Save
        PurchaseOrder savedOrder = purchaseOrderRepository.save(purchaseOrder);

        // 6. Convert to DTO
        PurchaseOrderResponseDto responseDto = PurchaseOrderMapper.toResponseDto(savedOrder);

        return new ApiResponse<>("Purchase order updated successfully", responseDto);
    }

    // ✅ APPROVE Purchase Order
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

    // ✅ RECEIVE Purchase Order (+ Update Inventory)
    @Transactional
    public ApiResponse<PurchaseOrderResponseDto> receivePurchaseOrder(Long id, Long warehouseId) {
        // 1. Find purchase order
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));

        // 2. Validate current status
        if (purchaseOrder.getStatus() != PurchaseOrderStatus.APPROVED) {
            throw new InvalidOperationException(
                    "Can only receive purchase orders with APPROVED status. Current status: " + purchaseOrder.getStatus()
            );
        }

        // 3. Find warehouse
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", warehouseId));

        // 4. Update inventory for each line
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
            inventoryRepository.save(inventory);
        }

        // 5. Update purchase order status
        purchaseOrder.setStatus(PurchaseOrderStatus.RECEIVED);
        purchaseOrder.setReceivedAt(LocalDateTime.now());

        PurchaseOrder savedOrder = purchaseOrderRepository.save(purchaseOrder);
        PurchaseOrderResponseDto responseDto = PurchaseOrderMapper.toResponseDto(savedOrder);

        return new ApiResponse<>("Purchase order received successfully and inventory updated", responseDto);
    }

    // ✅ CANCEL Purchase Order
    @Transactional
    public ApiResponse<PurchaseOrderResponseDto> cancelPurchaseOrder(Long id) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", "id", id));

        // Validate current status
        if (purchaseOrder.getStatus() == PurchaseOrderStatus.RECEIVED) {
            throw new InvalidOperationException(
                    "Cannot cancel a purchase order that has already been received"
            );
        }

        if (purchaseOrder.getStatus() == PurchaseOrderStatus.CANCELED) {
            throw new InvalidOperationException("Purchase order is already canceled");
        }

        // Update status
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
}