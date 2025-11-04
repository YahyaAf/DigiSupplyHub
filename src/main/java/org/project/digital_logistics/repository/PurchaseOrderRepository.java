package org.project.digital_logistics.repository;

import org.project.digital_logistics.enums.PurchaseOrderStatus;
import org.project.digital_logistics.model.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    List<PurchaseOrder> findByStatus(PurchaseOrderStatus status);

    List<PurchaseOrder> findBySupplierId(Long supplierId);

    List<PurchaseOrder> findBySupplierIdAndStatus(Long supplierId, PurchaseOrderStatus status);

    @Query("SELECT po FROM PurchaseOrder po WHERE po.expectedDelivery BETWEEN :start AND :end")
    List<PurchaseOrder> findByExpectedDeliveryBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    long countByStatus(PurchaseOrderStatus status);
}