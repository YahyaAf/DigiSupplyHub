package org.project.digital_logistics.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.warehouse.WarehouseRequestDto;
import org.project.digital_logistics.dto.warehouse.WarehouseResponseDto;
import org.project.digital_logistics.service.PermissionService;
import org.project.digital_logistics.service.WarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouses")
@CrossOrigin(origins = "*")
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final PermissionService permissionService;

    @Autowired
    public WarehouseController(WarehouseService warehouseService, PermissionService permissionService) {
        this.warehouseService = warehouseService;
        this.permissionService = permissionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WarehouseResponseDto>> createWarehouse(
            @Valid @RequestBody WarehouseRequestDto requestDto,
            HttpSession session) {

        permissionService.requireAdmin(session);

        ApiResponse<WarehouseResponseDto> response = warehouseService.createWarehouse(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WarehouseResponseDto>> getWarehouseById(
            @PathVariable Long id,
            HttpSession session) {

        permissionService.requireAdmin(session);

        ApiResponse<WarehouseResponseDto> response = warehouseService.getWarehouseById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<WarehouseResponseDto>> getWarehouseByCode(
            @PathVariable String code,
            HttpSession session) {

        permissionService.requireAdmin(session);

        ApiResponse<WarehouseResponseDto> response = warehouseService.getWarehouseByCode(code);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WarehouseResponseDto>>> getAllWarehouses(HttpSession session) {
        permissionService.requireAdmin(session);
        ApiResponse<List<WarehouseResponseDto>> response = warehouseService.getAllWarehouses();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WarehouseResponseDto>> updateWarehouse(
            @PathVariable Long id,
            @Valid @RequestBody WarehouseRequestDto requestDto,
            HttpSession session) {

        permissionService.requireAdmin(session);

        ApiResponse<WarehouseResponseDto> response = warehouseService.updateWarehouse(id, requestDto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteWarehouse(
            @PathVariable Long id,
            HttpSession session) {

        permissionService.requireAdmin(session);

        ApiResponse<Void> response = warehouseService.deleteWarehouse(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> countWarehouses(HttpSession session) {
        permissionService.requireAdmin(session);
        ApiResponse<Long> response = warehouseService.countWarehouses();
        return ResponseEntity.ok(response);
    }
}