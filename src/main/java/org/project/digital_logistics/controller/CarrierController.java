package org.project.digital_logistics.controller;

import jakarta.validation.Valid;
import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.carrier.CarrierRequestDto;
import org.project.digital_logistics.dto.carrier.CarrierResponseDto;
import org.project.digital_logistics.dto.shipment.ShipmentResponseDto;
import org.project.digital_logistics.model.enums.CarrierStatus;
import org.project.digital_logistics.service.CarrierService;
import org.project.digital_logistics.service.ShipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carriers")
@CrossOrigin(origins = "*")
public class CarrierController {

    private final CarrierService carrierService;
    private final ShipmentService shipmentService;

    @Autowired
    public CarrierController(CarrierService carrierService, ShipmentService shipmentService) {
        this.carrierService = carrierService;
        this.shipmentService = shipmentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CarrierResponseDto>> createCarrier(
            @Valid @RequestBody CarrierRequestDto requestDto) {
        ApiResponse<CarrierResponseDto> response = carrierService.createCarrier(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CarrierResponseDto>> getCarrierById(@PathVariable Long id) {
        ApiResponse<CarrierResponseDto> response = carrierService.getCarrierById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<CarrierResponseDto>> getCarrierByCode(@PathVariable String code) {
        ApiResponse<CarrierResponseDto> response = carrierService.getCarrierByCode(code);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CarrierResponseDto>>> getAllCarriers() {
        ApiResponse<List<CarrierResponseDto>> response = carrierService.getAllCarriers();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<CarrierResponseDto>>> getCarriersByStatus(
            @PathVariable CarrierStatus status) {
        ApiResponse<List<CarrierResponseDto>> response = carrierService.getCarriersByStatus(status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<CarrierResponseDto>>> getAvailableCarriers() {
        ApiResponse<List<CarrierResponseDto>> response = carrierService.getAvailableCarriers();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CarrierResponseDto>> updateCarrier(
            @PathVariable Long id,
            @Valid @RequestBody CarrierRequestDto requestDto) {
        ApiResponse<CarrierResponseDto> response = carrierService.updateCarrier(id, requestDto);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<CarrierResponseDto>> updateCarrierStatus(
            @PathVariable Long id,
            @RequestParam CarrierStatus status) {
        ApiResponse<CarrierResponseDto> response = carrierService.updateCarrierStatus(id, status);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{carrierId}/assign-shipment/{shipmentId}")
    public ResponseEntity<ApiResponse<ShipmentResponseDto>> assignShipment(
            @PathVariable Long carrierId,
            @PathVariable Long shipmentId) {
        ApiResponse<ShipmentResponseDto> response = shipmentService.assignCarrier(shipmentId, carrierId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{carrierId}/assign-multiple")
    public ResponseEntity<ApiResponse<List<ShipmentResponseDto>>> assignMultipleShipments(
            @PathVariable Long carrierId,
            @RequestBody List<Long> shipmentIds) {
        ApiResponse<List<ShipmentResponseDto>> response =
                shipmentService.assignMultipleShipments(carrierId, shipmentIds);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{carrierId}/shipments")
    public ResponseEntity<ApiResponse<List<ShipmentResponseDto>>> getCarrierShipments(
            @PathVariable Long carrierId) {
        ApiResponse<List<ShipmentResponseDto>> response = shipmentService.getShipmentsByCarrier(carrierId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCarrier(@PathVariable Long id) {
        ApiResponse<Void> response = carrierService.deleteCarrier(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> countCarriers() {
        ApiResponse<Long> response = carrierService.countCarriers();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-daily-shipments")
    public ResponseEntity<ApiResponse<Void>> resetDailyShipments() {
        ApiResponse<Void> response = carrierService.resetDailyShipments();
        return ResponseEntity.ok(response);
    }
}