package org.project.digital_logistics.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.carrier.CarrierRequestDto;
import org.project.digital_logistics.dto.carrier.CarrierResponseDto;
import org.project.digital_logistics.dto.shipment.ShipmentResponseDto;
import org.project.digital_logistics.model.enums.CarrierStatus;
import org.project.digital_logistics.service.CarrierService;
import org.project.digital_logistics.service.PermissionService;
import org.project.digital_logistics.service.ShipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carriers")
@CrossOrigin(origins = "*")
@Tag(name = "Carriers", description = "Carrier Management - ADMIN for write, ADMIN+WAREHOUSE_MANAGER for read")
public class CarrierController {

    private final CarrierService carrierService;
    private final ShipmentService shipmentService;
    private final PermissionService permissionService;

    @Autowired
    public CarrierController(CarrierService carrierService,
                             ShipmentService shipmentService,
                             PermissionService permissionService) {
        this.carrierService = carrierService;
        this.shipmentService = shipmentService;
        this.permissionService = permissionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CarrierResponseDto>> createCarrier(
            @Valid @RequestBody CarrierRequestDto requestDto,
            HttpSession session) {

        permissionService.requireAdmin(session);

        ApiResponse<CarrierResponseDto> response = carrierService.createCarrier(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CarrierResponseDto>> getCarrierById(
            @PathVariable Long id,
            HttpSession session) {

        permissionService.requireWarehouseManager(session);

        ApiResponse<CarrierResponseDto> response = carrierService.getCarrierById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<CarrierResponseDto>> getCarrierByCode(
            @PathVariable String code,
            HttpSession session) {

        permissionService.requireWarehouseManager(session);

        ApiResponse<CarrierResponseDto> response = carrierService.getCarrierByCode(code);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CarrierResponseDto>>> getAllCarriers(HttpSession session) {
        permissionService.requireWarehouseManager(session);
        ApiResponse<List<CarrierResponseDto>> response = carrierService.getAllCarriers();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<CarrierResponseDto>>> getCarriersByStatus(
            @PathVariable CarrierStatus status,
            HttpSession session) {

        permissionService.requireWarehouseManager(session);

        ApiResponse<List<CarrierResponseDto>> response = carrierService.getCarriersByStatus(status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<CarrierResponseDto>>> getAvailableCarriers(HttpSession session) {
        permissionService.requireWarehouseManager(session);
        ApiResponse<List<CarrierResponseDto>> response = carrierService.getAvailableCarriers();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CarrierResponseDto>> updateCarrier(
            @PathVariable Long id,
            @Valid @RequestBody CarrierRequestDto requestDto,
            HttpSession session) {

        permissionService.requireAdmin(session);

        ApiResponse<CarrierResponseDto> response = carrierService.updateCarrier(id, requestDto);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<CarrierResponseDto>> updateCarrierStatus(
            @PathVariable Long id,
            @RequestParam CarrierStatus status,
            HttpSession session) {

        permissionService.requireAdmin(session);

        ApiResponse<CarrierResponseDto> response = carrierService.updateCarrierStatus(id, status);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{carrierId}/assign-shipment/{shipmentId}")
    public ResponseEntity<ApiResponse<ShipmentResponseDto>> assignShipment(
            @PathVariable Long carrierId,
            @PathVariable Long shipmentId,
            HttpSession session) {

        permissionService.requireWarehouseManager(session);

        ApiResponse<ShipmentResponseDto> response = shipmentService.assignCarrier(shipmentId, carrierId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{carrierId}/assign-multiple")
    public ResponseEntity<ApiResponse<List<ShipmentResponseDto>>> assignMultipleShipments(
            @PathVariable Long carrierId,
            @RequestBody List<Long> shipmentIds,
            HttpSession session) {

        permissionService.requireWarehouseManager(session);

        ApiResponse<List<ShipmentResponseDto>> response =
                shipmentService.assignMultipleShipments(carrierId, shipmentIds);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{carrierId}/shipments")
    public ResponseEntity<ApiResponse<List<ShipmentResponseDto>>> getCarrierShipments(
            @PathVariable Long carrierId,
            HttpSession session) {

        permissionService.requireWarehouseManager(session);

        ApiResponse<List<ShipmentResponseDto>> response = shipmentService.getShipmentsByCarrier(carrierId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCarrier(
            @PathVariable Long id,
            HttpSession session) {

        permissionService.requireAdmin(session);

        ApiResponse<Void> response = carrierService.deleteCarrier(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> countCarriers(HttpSession session) {
        permissionService.requireWarehouseManager(session);
        ApiResponse<Long> response = carrierService.countCarriers();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-daily-shipments")
    public ResponseEntity<ApiResponse<Void>> resetDailyShipments(HttpSession session) {
        permissionService.requireAdmin(session);
        ApiResponse<Void> response = carrierService.resetDailyShipments();
        return ResponseEntity.ok(response);
    }
}