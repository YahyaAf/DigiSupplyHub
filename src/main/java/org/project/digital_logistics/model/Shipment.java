package org.project.digital_logistics.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.project.digital_logistics.model.enums.ShipmentStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "shipments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_id", nullable = false, unique = true)
    private SalesOrder salesOrder;

    @Column(name = "tracking_number", unique = true, nullable = false, length = 100)
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ShipmentStatus status = ShipmentStatus.PLANNED;

    @Column(name = "planned_date")
    private LocalDateTime plannedDate;

    @Column(name = "shipped_date")
    private LocalDateTime shippedDate;

    @Column(name = "delivered_date")
    private LocalDateTime deliveredDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (trackingNumber == null) {
            trackingNumber = "TRK-" + System.currentTimeMillis();
        }
    }

    @Override
    public String toString() {
        return "Shipment{" +
                "id=" + id +
                ", salesOrderId=" + (salesOrder != null ? salesOrder.getId() : null) +
                ", trackingNumber='" + trackingNumber + '\'' +
                ", status=" + status +
                ", plannedDate=" + plannedDate +
                ", shippedDate=" + shippedDate +
                ", deliveredDate=" + deliveredDate +
                '}';
    }
}