package org.project.digital_logistics.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventories",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"warehouse_id", "product_id"})
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    @Builder.Default
    private Integer qtyOnHand = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer qtyReserved = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Inventory{" +
                "id=" + id +
                ", warehouseId=" + (warehouse != null ? warehouse.getId() : null) +
                ", productId=" + (product != null ? product.getId() : null) +
                ", qtyOnHand=" + qtyOnHand +
                ", qtyReserved=" + qtyReserved +
                '}';
    }

    public Integer getAvailableQuantity() {
        return qtyOnHand - qtyReserved;
    }
}