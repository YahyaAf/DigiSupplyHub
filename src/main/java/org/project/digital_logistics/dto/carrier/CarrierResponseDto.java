package org.project.digital_logistics.dto.carrier;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.project.digital_logistics.model.enums.CarrierStatus;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarrierResponseDto {

    private Long id;
    private String code;
    private String name;
    private String contactEmail;
    private String contactPhone;
    private BigDecimal baseShippingRate;
    private Integer maxDailyCapacity;
    private Integer currentDailyShipments;
    private LocalTime cutOffTime;
    private CarrierStatus status;

    private Integer availableCapacity;
}