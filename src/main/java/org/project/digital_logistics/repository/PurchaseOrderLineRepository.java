package org.project.digital_logistics.repository;

import org.project.digital_logistics.model.PurchaseOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderLineRepository extends JpaRepository<PurchaseOrderLine, Long> {

    List<PurchaseOrderLine> findByPurchaseOrderId(Long purchaseOrderId);

    List<PurchaseOrderLine> findByProductId(Long productId);

    void deleteByPurchaseOrderId(Long purchaseOrderId);
}