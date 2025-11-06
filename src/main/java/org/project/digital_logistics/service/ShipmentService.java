package org.project.digital_logistics.service;

import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.shipment.ShipmentResponseDto;
import org.project.digital_logistics.model.Carrier;
import org.project.digital_logistics.model.enums.OrderStatus;
import org.project.digital_logistics.model.enums.ShipmentStatus;
import org.project.digital_logistics.model.enums.CarrierStatus;
import org.project.digital_logistics.exception.InvalidOperationException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.mapper.ShipmentMapper;
import org.project.digital_logistics.model.SalesOrder;
import org.project.digital_logistics.model.Shipment;
import org.project.digital_logistics.repository.CarrierRepository;
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
    private final CarrierRepository carrierRepository;
    private final CarrierService carrierService;

    @Autowired
    public ShipmentService(ShipmentRepository shipmentRepository,
                           SalesOrderRepository salesOrderRepository,
                           CarrierRepository carrierRepository,
                           CarrierService carrierService) {
        this.shipmentRepository = shipmentRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.carrierRepository = carrierRepository;
        this.carrierService = carrierService;
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

        if (shipment.getCarrier() != null) {
            carrierService.decrementDailyShipments(shipment.getCarrier().getId());
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

    @Transactional
    public ApiResponse<ShipmentResponseDto> assignCarrier(Long shipmentId, Long carrierId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", "id", shipmentId));

        if (shipment.getStatus() != ShipmentStatus.PLANNED) {
            throw new InvalidOperationException(
                    "Can only assign carrier to PLANNED shipments. Current status: " + shipment.getStatus()
            );
        }

        Carrier carrier = carrierRepository.findById(carrierId)
                .orElseThrow(() -> new ResourceNotFoundException("Carrier", "id", carrierId));

        if (carrier.getStatus() != CarrierStatus.ACTIVE) {
            throw new InvalidOperationException(
                    "Cannot assign shipment to carrier with status: " + carrier.getStatus()
            );
        }

        if (carrier.getCurrentDailyShipments() >= carrier.getMaxDailyCapacity()) {
            throw new InvalidOperationException(
                    "Carrier has reached max daily capacity: " + carrier.getMaxDailyCapacity()
            );
        }

        shipment.setCarrier(carrier);
        shipment.setStatus(ShipmentStatus.IN_TRANSIT);
        Shipment savedShipment = shipmentRepository.save(shipment);

        carrierService.incrementDailyShipments(carrierId);

        ShipmentResponseDto responseDto = ShipmentMapper.toResponseDto(savedShipment);
        return new ApiResponse<>(
                "Carrier " + carrier.getName() + " assigned to shipment successfully",
                responseDto
        );
    }

    @Transactional
    public ApiResponse<List<ShipmentResponseDto>> assignMultipleShipments(Long carrierId, List<Long> shipmentIds) {
        Carrier carrier = carrierRepository.findById(carrierId)
                .orElseThrow(() -> new ResourceNotFoundException("Carrier", "id", carrierId));

        if (carrier.getStatus() != CarrierStatus.ACTIVE) {
            throw new InvalidOperationException("Carrier is not ACTIVE");
        }

        int availableCapacity = carrier.getMaxDailyCapacity() - carrier.getCurrentDailyShipments();
        if (shipmentIds.size() > availableCapacity) {
            throw new InvalidOperationException(
                    "Cannot assign " + shipmentIds.size() + " shipments. Available capacity: " + availableCapacity
            );
        }

        List<Shipment> assignedShipments = shipmentIds.stream()
                .map(shipmentId -> {
                    Shipment shipment = shipmentRepository.findById(shipmentId)
                            .orElseThrow(() -> new ResourceNotFoundException("Shipment", "id", shipmentId));

                    if (shipment.getStatus() != ShipmentStatus.PLANNED) {
                        throw new InvalidOperationException(
                                "Shipment " + shipmentId + " is not PLANNED. Current status: " + shipment.getStatus()
                        );
                    }

                    shipment.setStatus(ShipmentStatus.IN_TRANSIT);
                    shipment.setCarrier(carrier);
                    return shipmentRepository.save(shipment);
                })
                .toList();

        carrier.setCurrentDailyShipments(carrier.getCurrentDailyShipments() + shipmentIds.size());
        carrierRepository.save(carrier);

        List<ShipmentResponseDto> responseDtos = assignedShipments.stream()
                .map(ShipmentMapper::toResponseDto)
                .toList();

        return new ApiResponse<>(
                shipmentIds.size() + " shipments assigned to carrier " + carrier.getName() + " successfully",
                responseDtos
        );
    }

    public ApiResponse<List<ShipmentResponseDto>> getShipmentsByCarrier(Long carrierId) {
        if (!carrierRepository.existsById(carrierId)) {
            throw new ResourceNotFoundException("Carrier", "id", carrierId);
        }

        List<ShipmentResponseDto> shipments = shipmentRepository.findByCarrierId(carrierId)
                .stream()
                .map(ShipmentMapper::toResponseDto)
                .toList();
        return new ApiResponse<>("Carrier shipments retrieved successfully", shipments);
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