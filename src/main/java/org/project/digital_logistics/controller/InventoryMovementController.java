package org.project.digital_logistics.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.inventorymovement.InventoryMovementRequestDto;
import org.project.digital_logistics.dto.inventorymovement.InventoryMovementResponseDto;
import org.project.digital_logistics.service.InventoryMovementService;
import org.project.digital_logistics.service.PermissionService;
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
    private final PermissionService permissionService;

    @Autowired
    public InventoryMovementController(InventoryMovementService movementService,
                                       PermissionService permissionService) {
        this.movementService = movementService;
        this.permissionService = permissionService;
    }

//    @PostMapping
//    public ResponseEntity<ApiResponse<InventoryMovementResponseDto>> createMovement(
//            @Valid @RequestBody InventoryMovementRequestDto requestDto) {
//        ApiResponse<InventoryMovementResponseDto> response = movementService.createMovement(requestDto);
//        return new ResponseEntity<>(response, HttpStatus.CREATED);
//    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InventoryMovementResponseDto>>> getAllMovements(
            HttpSession session) {

        permissionService.requireWarehouseManager(session);

        ApiResponse<List<InventoryMovementResponseDto>> response = movementService.getAllMovements();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/inventory/{inventoryId}")
    public ResponseEntity<ApiResponse<List<InventoryMovementResponseDto>>> getMovementsByInventory(
            @PathVariable Long inventoryId,
            HttpSession session) {

        permissionService.requireWarehouseManager(session);

        ApiResponse<List<InventoryMovementResponseDto>> response =
                movementService.getMovementsByInventory(inventoryId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/warehouse/{warehouseId}")
    public ResponseEntity<ApiResponse<List<InventoryMovementResponseDto>>> getMovementsByWarehouse(
            @PathVariable Long warehouseId,
            HttpSession session) {

        permissionService.requireWarehouseManager(session);

        ApiResponse<List<InventoryMovementResponseDto>> response =
                movementService.getMovementsByWarehouse(warehouseId);
        return ResponseEntity.ok(response);
    }
}