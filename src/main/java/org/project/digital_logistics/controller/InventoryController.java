package org.project.digital_logistics.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.project.digital_logistics.dto.ApiResponse;
import org.project. digital_logistics.dto.inventory. InventoryRequestDto;
import org.project.digital_logistics. dto.inventory.InventoryResponseDto;
import org.project. digital_logistics.service.InventoryService;
import org.springframework.beans.factory.annotation. Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventories")
@CrossOrigin(origins = "*")
@Tag(name = "Inventories", description = "Inventory Management")
public class InventoryController {

    private final InventoryService inventoryService;

    @Autowired
    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InventoryResponseDto>> createInventory(@Valid @RequestBody InventoryRequestDto requestDto) {
        ApiResponse<InventoryResponseDto> response = inventoryService.createInventory(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryResponseDto>> getInventoryById(@PathVariable Long id) {
        ApiResponse<InventoryResponseDto> response = inventoryService.getInventoryById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/warehouse/{warehouseId}/product/{productId}")
    public ResponseEntity<ApiResponse<InventoryResponseDto>> getInventoryByWarehouseAndProduct(
            @PathVariable Long warehouseId,
            @PathVariable Long productId) {
        ApiResponse<InventoryResponseDto> response = inventoryService.getInventoryByWarehouseAndProduct(warehouseId, productId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InventoryResponseDto>>> getAllInventories() {
        ApiResponse<List<InventoryResponseDto>> response = inventoryService.getAllInventories();
        return ResponseEntity. ok(response);
    }

    @GetMapping("/warehouse/{warehouseId}")
    public ResponseEntity<ApiResponse<List<InventoryResponseDto>>> getInventoriesByWarehouse(@PathVariable Long warehouseId) {
        ApiResponse<List<InventoryResponseDto>> response = inventoryService.getInventoriesByWarehouse(warehouseId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<List<InventoryResponseDto>>> getInventoriesByProduct(@PathVariable Long productId) {
        ApiResponse<List<InventoryResponseDto>> response = inventoryService.getInventoriesByProduct(productId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/warehouse/{warehouseId}/low-stock")
    public ResponseEntity<ApiResponse<List<InventoryResponseDto>>> getLowStockInWarehouse(
            @PathVariable Long warehouseId,
            @RequestParam(required = false, defaultValue = "10") Integer threshold) {
        ApiResponse<List<InventoryResponseDto>> response = inventoryService.getLowStockInWarehouse(warehouseId, threshold);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryResponseDto>> updateInventory(
            @PathVariable Long id,
            @Valid @RequestBody InventoryRequestDto requestDto) {
        ApiResponse<InventoryResponseDto> response = inventoryService.updateInventory(id, requestDto);
        return ResponseEntity. ok(response);
    }

    @PatchMapping("/{id}/adjust")
    public ResponseEntity<ApiResponse<InventoryResponseDto>> adjustQuantities(
            @PathVariable Long id,
            @RequestParam(required = false) Integer qtyOnHand,
            @RequestParam(required = false) Integer qtyReserved) {
        ApiResponse<InventoryResponseDto> response = inventoryService.adjustQuantities(id, qtyOnHand, qtyReserved);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInventory(@PathVariable Long id) {
        ApiResponse<Void> response = inventoryService.deleteInventory(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/warehouse/{warehouseId}/product/{productId}")
    public ResponseEntity<ApiResponse<Void>> deleteInventoryByWarehouseAndProduct(
            @PathVariable Long warehouseId,
            @PathVariable Long productId) {
        ApiResponse<Void> response = inventoryService.deleteInventoryByWarehouseAndProduct(warehouseId, productId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/product/{productId}/total-stock")
    public ResponseEntity<ApiResponse<Integer>> getTotalStockByProduct(@PathVariable Long productId) {
        ApiResponse<Integer> response = inventoryService. getTotalStockByProduct(productId);
        return ResponseEntity. ok(response);
    }

    @GetMapping("/product/{productId}/available-stock")
    public ResponseEntity<ApiResponse<Integer>> getAvailableStockByProduct(@PathVariable Long productId) {
        ApiResponse<Integer> response = inventoryService.getAvailableStockByProduct(productId);
        return ResponseEntity. ok(response);
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> countInventories() {
        ApiResponse<Long> response = inventoryService.countInventories();
        return ResponseEntity.ok(response);
    }
}