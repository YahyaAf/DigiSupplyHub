package org.project.digital_logistics.repository;

import org.project.digital_logistics.model.Inventory;
import org.project.digital_logistics.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByWarehouseIdAndProductId(Long warehouseId, Long productId);
    boolean existsByWarehouseIdAndProductId(Long warehouseId, Long productId);
    List<Inventory> findByWarehouseId(Long warehouseId);
    List<Inventory> findByProductId(Long productId);
    boolean existsByProduct(Product product);

    @Query("SELECT i FROM Inventory i WHERE i.warehouse.id = :warehouseId " +
            "AND (i.qtyOnHand - i.qtyReserved) < :threshold")
    List<Inventory> findLowStockInWarehouse(@Param("warehouseId") Long warehouseId,
                                            @Param("threshold") Integer threshold);

    @Query("SELECT COALESCE(SUM(i.qtyOnHand), 0) FROM Inventory i WHERE i.product.id = :productId")
    Integer getTotalStockByProduct(@Param("productId") Long productId);

    @Query("SELECT COALESCE(SUM(i.qtyOnHand - i.qtyReserved), 0) FROM Inventory i WHERE i.product.id = :productId")
    Integer getAvailableStockByProduct(@Param("productId") Long productId);

    void deleteByWarehouseIdAndProductId(Long warehouseId, Long productId);
}