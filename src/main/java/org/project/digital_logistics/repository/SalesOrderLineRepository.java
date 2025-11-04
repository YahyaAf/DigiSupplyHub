package org.project.digital_logistics.repository;

import org.project.digital_logistics.model.SalesOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesOrderLineRepository extends JpaRepository<SalesOrderLine, Long> {

    List<SalesOrderLine> findBySalesOrderId(Long salesOrderId);

    List<SalesOrderLine> findByProductId(Long productId);
}