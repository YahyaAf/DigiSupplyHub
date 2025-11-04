package org.project.digital_logistics.controller;

import jakarta.validation.Valid;
import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.purchaseorder.PurchaseOrderRequestDto;
import org.project.digital_logistics.dto.purchaseorder.PurchaseOrderResponseDto;
import org.project.digital_logistics.enums.PurchaseOrderStatus;
import org.project.digital_logistics.service.PurchaseOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
@CrossOrigin(origins = "*")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @Autowired
    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseOrderResponseDto>> createPurchaseOrder(
            @Valid @RequestBody PurchaseOrderRequestDto requestDto) {
        ApiResponse<PurchaseOrderResponseDto> response = purchaseOrderService.createPurchaseOrder(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseOrderResponseDto>> getPurchaseOrderById(@PathVariable Long id) {
        ApiResponse<PurchaseOrderResponseDto> response = purchaseOrderService.getPurchaseOrderById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PurchaseOrderResponseDto>>> getAllPurchaseOrders() {
        ApiResponse<List<PurchaseOrderResponseDto>> response = purchaseOrderService.getAllPurchaseOrders();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<PurchaseOrderResponseDto>>> getPurchaseOrdersByStatus(
            @PathVariable PurchaseOrderStatus status) {
        ApiResponse<List<PurchaseOrderResponseDto>> response = purchaseOrderService.getPurchaseOrdersByStatus(status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<ApiResponse<List<PurchaseOrderResponseDto>>> getPurchaseOrdersBySupplier(
            @PathVariable Long supplierId) {
        ApiResponse<List<PurchaseOrderResponseDto>> response = purchaseOrderService.getPurchaseOrdersBySupplier(supplierId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseOrderResponseDto>> updatePurchaseOrder(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseOrderRequestDto requestDto) {
        ApiResponse<PurchaseOrderResponseDto> response = purchaseOrderService.updatePurchaseOrder(id, requestDto);
        return ResponseEntity.ok(response);
    }

    // ✅ APPROVE Purchase Order
    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<PurchaseOrderResponseDto>> approvePurchaseOrder(@PathVariable Long id) {
        ApiResponse<PurchaseOrderResponseDto> response = purchaseOrderService.approvePurchaseOrder(id);
        return ResponseEntity.ok(response);
    }

    // ✅ RECEIVE Purchase Order (+ Update Inventory)
    @PatchMapping("/{id}/receive")
    public ResponseEntity<ApiResponse<PurchaseOrderResponseDto>> receivePurchaseOrder(
            @PathVariable Long id,
            @RequestParam Long warehouseId) {
        ApiResponse<PurchaseOrderResponseDto> response = purchaseOrderService.receivePurchaseOrder(id, warehouseId);
        return ResponseEntity.ok(response);
    }

    // ✅ CANCEL Purchase Order
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<PurchaseOrderResponseDto>> cancelPurchaseOrder(@PathVariable Long id) {
        ApiResponse<PurchaseOrderResponseDto> response = purchaseOrderService.cancelPurchaseOrder(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePurchaseOrder(@PathVariable Long id) {
        ApiResponse<Void> response = purchaseOrderService.deletePurchaseOrder(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> countPurchaseOrders() {
        ApiResponse<Long> response = purchaseOrderService.countPurchaseOrders();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/count/status/{status}")
    public ResponseEntity<ApiResponse<Long>> countPurchaseOrdersByStatus(@PathVariable PurchaseOrderStatus status) {
        ApiResponse<Long> response = purchaseOrderService.countPurchaseOrdersByStatus(status);
        return ResponseEntity.ok(response);
    }
}