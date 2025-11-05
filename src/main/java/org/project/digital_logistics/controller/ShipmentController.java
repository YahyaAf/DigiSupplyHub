package org.project.digital_logistics.controller;

import jakarta.validation.Valid;
import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.shipment.ShipmentRequestDto;
import org.project.digital_logistics.dto.shipment.ShipmentResponseDto;
import org.project.digital_logistics.model.enums.ShipmentStatus;
import org.project.digital_logistics.service.ShipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/shipments")
@CrossOrigin(origins = "*")
public class ShipmentController {

    private final ShipmentService shipmentService;

    @Autowired
    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShipmentResponseDto>> getShipmentById(@PathVariable Long id) {
        ApiResponse<ShipmentResponseDto> response = shipmentService.getShipmentById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sales-order/{salesOrderId}")
    public ResponseEntity<ApiResponse<ShipmentResponseDto>> getShipmentBySalesOrder(
            @PathVariable Long salesOrderId) {
        ApiResponse<ShipmentResponseDto> response = shipmentService.getShipmentBySalesOrder(salesOrderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/track/{trackingNumber}")
    public ResponseEntity<ApiResponse<ShipmentResponseDto>> trackShipment(
            @PathVariable String trackingNumber) {
        ApiResponse<ShipmentResponseDto> response = shipmentService.getShipmentByTrackingNumber(trackingNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShipmentResponseDto>>> getAllShipments() {
        ApiResponse<List<ShipmentResponseDto>> response = shipmentService.getAllShipments();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<ShipmentResponseDto>>> getShipmentsByStatus(
            @PathVariable ShipmentStatus status) {
        ApiResponse<List<ShipmentResponseDto>> response = shipmentService.getShipmentsByStatus(status);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/in-transit")
    public ResponseEntity<ApiResponse<ShipmentResponseDto>> markAsInTransit(@PathVariable Long id) {
        ApiResponse<ShipmentResponseDto> response = shipmentService.markAsInTransit(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/deliver")
    public ResponseEntity<ApiResponse<ShipmentResponseDto>> markAsDelivered(@PathVariable Long id) {
        ApiResponse<ShipmentResponseDto> response = shipmentService.markAsDelivered(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/planned-date")
    public ResponseEntity<ApiResponse<ShipmentResponseDto>> updatePlannedDate(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime plannedDate) {
        ApiResponse<ShipmentResponseDto> response = shipmentService.updatePlannedDate(id, plannedDate);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteShipment(@PathVariable Long id) {
        ApiResponse<Void> response = shipmentService.deleteShipment(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> countShipments() {
        ApiResponse<Long> response = shipmentService.countShipments();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/count/status/{status}")
    public ResponseEntity<ApiResponse<Long>> countShipmentsByStatus(@PathVariable ShipmentStatus status) {
        ApiResponse<Long> response = shipmentService.countShipmentsByStatus(status);
        return ResponseEntity.ok(response);
    }
}