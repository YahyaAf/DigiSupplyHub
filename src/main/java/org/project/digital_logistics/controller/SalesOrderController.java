package org.project.digital_logistics.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.salesorder.SalesOrderRequestDto;
import org.project.digital_logistics.dto.salesorder.SalesOrderResponseDto;
import org.project.digital_logistics.model.enums.OrderStatus;
import org.project.digital_logistics.service.SalesOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales-orders")
@CrossOrigin(origins = "*")
public class SalesOrderController {

    private static final String SESSION_USER_KEY = "authenticated_user";

    private final SalesOrderService salesOrderService;

    @Autowired
    public SalesOrderController(SalesOrderService salesOrderService) {
        this.salesOrderService = salesOrderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SalesOrderResponseDto>> createSalesOrder(
            @Valid @RequestBody SalesOrderRequestDto requestDto,
            HttpSession session) {

        Long authenticatedClientId = (Long) session.getAttribute(SESSION_USER_KEY);

        if (authenticatedClientId == null) {
            throw new IllegalStateException("Not authenticated. Please login.");
        }

        ApiResponse<SalesOrderResponseDto> response =
                salesOrderService.createSalesOrder(requestDto, authenticatedClientId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SalesOrderResponseDto>> getSalesOrderById(@PathVariable Long id) {
        ApiResponse<SalesOrderResponseDto> response = salesOrderService.getSalesOrderById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SalesOrderResponseDto>>> getAllSalesOrders() {
        ApiResponse<List<SalesOrderResponseDto>> response = salesOrderService.getAllSalesOrders();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-orders")
    public ResponseEntity<ApiResponse<List<SalesOrderResponseDto>>> getMyOrders(HttpSession session) {
        Long authenticatedClientId = (Long) session.getAttribute(SESSION_USER_KEY);

        if (authenticatedClientId == null) {
            throw new IllegalStateException("Not authenticated. Please login.");
        }

        ApiResponse<List<SalesOrderResponseDto>> response =
                salesOrderService.getSalesOrdersByClient(authenticatedClientId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<SalesOrderResponseDto>>> getSalesOrdersByStatus(
            @PathVariable OrderStatus status) {
        ApiResponse<List<SalesOrderResponseDto>> response = salesOrderService.getSalesOrdersByStatus(status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<ApiResponse<List<SalesOrderResponseDto>>> getSalesOrdersByClient(
            @PathVariable Long clientId) {
        ApiResponse<List<SalesOrderResponseDto>> response = salesOrderService.getSalesOrdersByClient(clientId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/reserve")
    public ResponseEntity<ApiResponse<SalesOrderResponseDto>> reserveStock(@PathVariable Long id) {
        ApiResponse<SalesOrderResponseDto> response = salesOrderService.reserveStock(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/ship")
    public ResponseEntity<ApiResponse<SalesOrderResponseDto>> shipOrder(@PathVariable Long id) {
        ApiResponse<SalesOrderResponseDto> response = salesOrderService.shipOrder(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/deliver")
    public ResponseEntity<ApiResponse<SalesOrderResponseDto>> deliverOrder(@PathVariable Long id) {
        ApiResponse<SalesOrderResponseDto> response = salesOrderService.deliverOrder(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<SalesOrderResponseDto>> cancelOrder(@PathVariable Long id) {
        ApiResponse<SalesOrderResponseDto> response = salesOrderService.cancelOrder(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> countSalesOrders() {
        ApiResponse<Long> response = salesOrderService.countSalesOrders();
        return ResponseEntity.ok(response);
    }
}