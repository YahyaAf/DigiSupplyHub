package org.project.digital_logistics.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.supplier.SupplierRequestDto;
import org.project.digital_logistics.dto.supplier.SupplierResponseDto;
import org.project.digital_logistics.service.PermissionService;
import org.project.digital_logistics.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@CrossOrigin(origins = "*")
public class SupplierController {

    private final SupplierService supplierService;
    private final PermissionService permissionService;

    @Autowired
    public SupplierController(SupplierService supplierService, PermissionService permissionService) {
        this.supplierService = supplierService;
        this.permissionService = permissionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SupplierResponseDto>> createSupplier(
            @Valid @RequestBody SupplierRequestDto requestDto,
            HttpSession session) {

        permissionService.requireAdmin(session);

        ApiResponse<SupplierResponseDto> response = supplierService.createSupplier(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierResponseDto>> getSupplierById(
            @PathVariable Long id,
            HttpSession session) {

        permissionService.requireAdmin(session);

        ApiResponse<SupplierResponseDto> response = supplierService.getSupplierById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SupplierResponseDto>>> getAllSuppliers(HttpSession session) {
        permissionService.requireAdmin(session);
        ApiResponse<List<SupplierResponseDto>> response = supplierService.getAllSuppliers();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierResponseDto>> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody SupplierRequestDto requestDto,
            HttpSession session) {

        permissionService.requireAdmin(session);

        ApiResponse<SupplierResponseDto> response = supplierService.updateSupplier(id, requestDto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSupplier(
            @PathVariable Long id,
            HttpSession session) {

        permissionService.requireAdmin(session);

        ApiResponse<Void> response = supplierService.deleteSupplier(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> countSuppliers(HttpSession session) {
        permissionService.requireAdmin(session);
        ApiResponse<Long> response = supplierService.countSuppliers();
        return ResponseEntity.ok(response);
    }
}