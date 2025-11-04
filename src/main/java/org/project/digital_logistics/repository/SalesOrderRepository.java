package org.project.digital_logistics.repository;

import org.project.digital_logistics.model.enums.OrderStatus;
import org.project.digital_logistics.model.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

    List<SalesOrder> findByStatus(OrderStatus status);

    List<SalesOrder> findByClientId(Long clientId);

    List<SalesOrder> findByWarehouseId(Long warehouseId);

    long countByStatus(OrderStatus status);
}