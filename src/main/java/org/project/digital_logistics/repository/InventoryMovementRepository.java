package org.project.digital_logistics.repository;

import org.project.digital_logistics.model.enums.MovementType;
import org.project.digital_logistics.model.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    List<InventoryMovement> findByInventoryId(Long inventoryId);
    List<InventoryMovement> findByType(MovementType type);
    List<InventoryMovement> findByReferenceDocument(String referenceDocument);
    List<InventoryMovement> findByOccurredAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT im FROM InventoryMovement im " +
            "WHERE im.inventory.warehouse.id = :warehouseId " +
            "ORDER BY im.occurredAt DESC")
    List<InventoryMovement> findByWarehouseId(@Param("warehouseId") Long warehouseId);

    @Query("SELECT im FROM InventoryMovement im " +
            "WHERE im.inventory.product.id = :productId " +
            "ORDER BY im.occurredAt DESC")
    List<InventoryMovement> findByProductId(@Param("productId") Long productId);

    @Query("SELECT im FROM InventoryMovement im " +
            "ORDER BY im.occurredAt DESC")
    List<InventoryMovement> findRecentMovements();
}