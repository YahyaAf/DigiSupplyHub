package org.project.digital_logistics.service;

import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.shipment.ShipmentRequestDto;
import org.project.digital_logistics.dto.shipment.ShipmentResponseDto;
import org.project.digital_logistics.model.enums.OrderStatus;
import org.project.digital_logistics.model.enums.ShipmentStatus;
import org.project.digital_logistics.exception.DuplicateResourceException;
import org.project.digital_logistics.exception.InvalidOperationException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.mapper.ShipmentMapper;
import org.project.digital_logistics.model.SalesOrder;
import org.project.digital_logistics.model.Shipment;
import org.project.digital_logistics.repository.SalesOrderRepository;
import org.project.digital_logistics.repository.ShipmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final SalesOrderRepository salesOrderRepository;

    @Autowired
    public ShipmentService(ShipmentRepository shipmentRepository,
                           SalesOrderRepository salesOrderRepository) {
        this.shipmentRepository = shipmentRepository;
        this.salesOrderRepository = salesOrderRepository;
    }

    @Transactional
    public Shipment autoCreateShipment(SalesOrder salesOrder) {
        if (shipmentRepository.existsBySalesOrderId(salesOrder.getId())) {
            return shipmentRepository.findBySalesOrderId(salesOrder.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Shipment not found"));
        }

        Shipment shipment = ShipmentMapper.createFromSalesOrder(salesOrder);
        return shipmentRepository.save(shipment);
    }

    public ApiResponse<ShipmentResponseDto> getShipmentById(Long id) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "id", id));

        ShipmentResponseDto responseDto = ShipmentMapper.toResponseDto(shipment);
        return new ApiResponse<>("Shipment retrieved successfully", responseDto);
    }

    public ApiResponse<ShipmentResponseDto> getShipmentBySalesOrder(Long salesOrderId) {
        Shipment shipment = shipmentRepository.findBySalesOrderId(salesOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Shipment not found for Sales Order ID: " + salesOrderId
                ));

        ShipmentResponseDto responseDto = ShipmentMapper.toResponseDto(shipment);
        return new ApiResponse<>("Shipment retrieved successfully", responseDto);
    }

    public ApiResponse<ShipmentResponseDto> getShipmentByTrackingNumber(String trackingNumber) {
        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Shipment", "trackingNumber", trackingNumber
                ));

        ShipmentResponseDto responseDto = ShipmentMapper.toResponseDto(shipment);
        return new ApiResponse<>("Shipment retrieved successfully", responseDto);
    }

    public ApiResponse<List<ShipmentResponseDto>> getAllShipments() {
        List<ShipmentResponseDto> shipments = shipmentRepository.findAll()
                .stream()
                .map(ShipmentMapper::toResponseDto)
                .toList();
        return new ApiResponse<>("Shipments retrieved successfully", shipments);
    }

    public ApiResponse<List<ShipmentResponseDto>> getShipmentsByStatus(ShipmentStatus status) {
        List<ShipmentResponseDto> shipments = shipmentRepository.findByStatus(status)
                .stream()
                .map(ShipmentMapper::toResponseDto)
                .toList();
        return new ApiResponse<>("Shipments retrieved successfully", shipments);
    }

    @Transactional
    public ApiResponse<ShipmentResponseDto> markAsInTransit(Long id) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "id", id));

        if (shipment.getStatus() != ShipmentStatus.PLANNED) {
            throw new InvalidOperationException(
                    "Can only mark PLANNED shipments as IN_TRANSIT. Current status: " + shipment.getStatus()
            );
        }

        shipment.setStatus(ShipmentStatus.IN_TRANSIT);
        shipment.setShippedDate(LocalDateTime.now());

        Shipment savedShipment = shipmentRepository.save(shipment);
        ShipmentResponseDto responseDto = ShipmentMapper.toResponseDto(savedShipment);

        return new ApiResponse<>("Shipment marked as IN_TRANSIT", responseDto);
    }

    @Transactional
    public ApiResponse<ShipmentResponseDto> markAsDelivered(Long id) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "id", id));

        if (shipment.getStatus() != ShipmentStatus.IN_TRANSIT) {
            throw new InvalidOperationException(
                    "Can only deliver IN_TRANSIT shipments. Current status: " + shipment.getStatus()
            );
        }

        shipment.setStatus(ShipmentStatus.DELIVERED);
        shipment.setDeliveredDate(LocalDateTime.now());

        SalesOrder salesOrder = shipment.getSalesOrder();
        if (salesOrder.getStatus() == OrderStatus.SHIPPED) {
            salesOrder.setStatus(OrderStatus.DELIVERED);
            salesOrder.setDeliveredAt(LocalDateTime.now());
            salesOrderRepository.save(salesOrder);
        }

        Shipment savedShipment = shipmentRepository.save(shipment);
        ShipmentResponseDto responseDto = ShipmentMapper.toResponseDto(savedShipment);

        return new ApiResponse<>("Shipment marked as DELIVERED and Sales Order updated", responseDto);
    }

    @Transactional
    public ApiResponse<ShipmentResponseDto> updatePlannedDate(Long id, LocalDateTime plannedDate) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "id", id));

        if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
            throw new InvalidOperationException("Cannot update planned date for delivered shipments");
        }

        shipment.setPlannedDate(plannedDate);

        Shipment savedShipment = shipmentRepository.save(shipment);
        ShipmentResponseDto responseDto = ShipmentMapper.toResponseDto(savedShipment);

        return new ApiResponse<>("Shipment planned date updated", responseDto);
    }

    @Transactional
    public ApiResponse<Void> deleteShipment(Long id) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "id", id));

        if (shipment.getStatus() != ShipmentStatus.PLANNED) {
            throw new InvalidOperationException(
                    "Can only delete PLANNED shipments. Current status: " + shipment.getStatus()
            );
        }

        shipmentRepository.deleteById(id);
        return new ApiResponse<>("Shipment deleted successfully", null);
    }

    public ApiResponse<Long> countShipments() {
        long count = shipmentRepository.count();
        return new ApiResponse<>("Total shipments counted successfully", count);
    }

    public ApiResponse<Long> countShipmentsByStatus(ShipmentStatus status) {
        long count = shipmentRepository.findByStatus(status).size();
        return new ApiResponse<>("Shipments counted successfully", count);
    }
}