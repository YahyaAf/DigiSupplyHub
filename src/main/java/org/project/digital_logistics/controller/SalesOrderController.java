package org.project.digital_logistics.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.salesorder.SalesOrderRequestDto;
import org.project.digital_logistics.dto.salesorder.SalesOrderResponseDto;
import org.project.digital_logistics.model.enums.OrderStatus;
import org.project.digital_logistics.model.enums.Role;
import org.project.digital_logistics.model.User;
import org.project.digital_logistics.service.PermissionService;
import org.project.digital_logistics.service.SalesOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales-orders")
@CrossOrigin(origins = "*")
@Tag(name = "Sales Orders", description = "Sales Order Management - CLIENT for create, ADMIN+WAREHOUSE_MANAGER for manage")
public class SalesOrderController {

    private final SalesOrderService salesOrderService;
    private final PermissionService permissionService;

    @Autowired
    public SalesOrderController(SalesOrderService salesOrderService, PermissionService permissionService) {
        this.salesOrderService = salesOrderService;
        this.permissionService = permissionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SalesOrderResponseDto>> createSalesOrder(
            @Valid @RequestBody SalesOrderRequestDto requestDto,
            HttpSession session) {

        User user = permissionService.getAuthenticatedUser(session);

        if (user.getRole() != Role.CLIENT) {
            throw new org.project.digital_logistics.exception.AccessDeniedException(
                    "Only clients can create sales orders"
            );
        }

        ApiResponse<SalesOrderResponseDto> response =
                salesOrderService.createSalesOrder(requestDto, user.getId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SalesOrderResponseDto>> getSalesOrderById(
            @PathVariable Long id,
            HttpSession session) {

        permissionService.requireWarehouseManager(session);

        ApiResponse<SalesOrderResponseDto> response = salesOrderService.getSalesOrderById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SalesOrderResponseDto>>> getAllSalesOrders(
            HttpSession session) {

        permissionService.requireWarehouseManager(session);

        ApiResponse<List<SalesOrderResponseDto>> response = salesOrderService.getAllSalesOrders();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-orders")
    public ResponseEntity<ApiResponse<List<SalesOrderResponseDto>>> getMyOrders(HttpSession session) {
        User user = permissionService.getAuthenticatedUser(session);

        if (user.getRole() != Role.CLIENT) {
            throw new org.project.digital_logistics.exception.AccessDeniedException(
                    "Only clients can view their orders"
            );
        }

        ApiResponse<List<SalesOrderResponseDto>> response =
                salesOrderService.getSalesOrdersByClient(user.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<SalesOrderResponseDto>>> getSalesOrdersByStatus(
            @PathVariable OrderStatus status,
            HttpSession session) {

        permissionService.requireWarehouseManager(session);

        ApiResponse<List<SalesOrderResponseDto>> response = salesOrderService.getSalesOrdersByStatus(status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<ApiResponse<List<SalesOrderResponseDto>>> getSalesOrdersByClient(
            @PathVariable Long clientId,
            HttpSession session) {

        permissionService.requireWarehouseManager(session);

        ApiResponse<List<SalesOrderResponseDto>> response = salesOrderService.getSalesOrdersByClient(clientId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/reserve")
    public ResponseEntity<ApiResponse<SalesOrderResponseDto>> reserveStock(
            @PathVariable Long id,
            HttpSession session) {

        permissionService.requireWarehouseManager(session);

        ApiResponse<SalesOrderResponseDto> response = salesOrderService.reserveStock(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/ship")
    public ResponseEntity<ApiResponse<SalesOrderResponseDto>> shipOrder(
            @PathVariable Long id,
            HttpSession session) {

        permissionService.requireWarehouseManager(session);

        ApiResponse<SalesOrderResponseDto> response = salesOrderService.shipOrder(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/deliver")
    public ResponseEntity<ApiResponse<SalesOrderResponseDto>> deliverOrder(
            @PathVariable Long id,
            HttpSession session) {

        permissionService.requireWarehouseManager(session);

        ApiResponse<SalesOrderResponseDto> response = salesOrderService.deliverOrder(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<SalesOrderResponseDto>> cancelOrder(
            @PathVariable Long id,
            HttpSession session) {

        permissionService.requireWarehouseManager(session);

        ApiResponse<SalesOrderResponseDto> response = salesOrderService.cancelOrder(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> countSalesOrders(HttpSession session) {
        permissionService.requireWarehouseManager(session);
        ApiResponse<Long> response = salesOrderService.countSalesOrders();
        return ResponseEntity.ok(response);
    }
}