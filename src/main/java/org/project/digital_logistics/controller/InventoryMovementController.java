package org.project.digital_logistics.controller;

import jakarta.validation.Valid;
import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.inventorymovement.InventoryMovementRequestDto;
import org.project.digital_logistics.dto.inventorymovement.InventoryMovementResponseDto;
import org.project.digital_logistics.service.InventoryMovementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory-movements")
@CrossOrigin(origins = "*")
public class InventoryMovementController {

    private final InventoryMovementService movementService;

    @Autowired
    public InventoryMovementController(InventoryMovementService movementService) {
        this.movementService = movementService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InventoryMovementResponseDto>> createMovement(
            @Valid @RequestBody InventoryMovementRequestDto requestDto) {
        ApiResponse<InventoryMovementResponseDto> response = movementService.createMovement(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InventoryMovementResponseDto>>> getAllMovements() {
        ApiResponse<List<InventoryMovementResponseDto>> response = movementService.getAllMovements();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/inventory/{inventoryId}")
    public ResponseEntity<ApiResponse<List<InventoryMovementResponseDto>>> getMovementsByInventory(
            @PathVariable Long inventoryId) {
        ApiResponse<List<InventoryMovementResponseDto>> response =
                movementService.getMovementsByInventory(inventoryId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/warehouse/{warehouseId}")
    public ResponseEntity<ApiResponse<List<InventoryMovementResponseDto>>> getMovementsByWarehouse(
            @PathVariable Long warehouseId) {
        ApiResponse<List<InventoryMovementResponseDto>> response =
                movementService.getMovementsByWarehouse(warehouseId);
        return ResponseEntity.ok(response);
    }
}