package org.project.digital_logistics.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.project.digital_logistics.model.enums.CarrierStatus;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carriers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Carrier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "base_shipping_rate", precision = 19, scale = 2)
    private BigDecimal baseShippingRate;

    @Column(name = "max_daily_capacity")
    private Integer maxDailyCapacity;

    @Column(name = "current_daily_shipments")
    @Builder.Default
    private Integer currentDailyShipments = 0;

    @Column(name = "cut_off_time")
    private LocalTime cutOffTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CarrierStatus status = CarrierStatus.ACTIVE;

    @OneToMany(mappedBy = "carrier", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Shipment> shipments = new ArrayList<>();

    @Override
    public String toString() {
        return "Carrier{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", currentDailyShipments=" + currentDailyShipments +
                ", maxDailyCapacity=" + maxDailyCapacity +
                '}';
    }
}