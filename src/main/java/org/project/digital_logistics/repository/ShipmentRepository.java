package org.project.digital_logistics.repository;

import org.project.digital_logistics.model.enums.ShipmentStatus;
import org.project.digital_logistics.model.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    Optional<Shipment> findBySalesOrderId(Long salesOrderId);

    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    List<Shipment> findByStatus(ShipmentStatus status);

    boolean existsBySalesOrderId(Long salesOrderId);
}