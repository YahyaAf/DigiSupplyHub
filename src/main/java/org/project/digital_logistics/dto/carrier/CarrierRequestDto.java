package org.project.digital_logistics.dto.carrier;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarrierRequestDto {

    @NotBlank(message = "Carrier code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    private String code;

    @NotBlank(message = "Carrier name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String contactEmail;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String contactPhone;

    @DecimalMin(value = "0.0", message = "Base shipping rate must be positive")
    @Digits(integer = 17, fraction = 2)
    private BigDecimal baseShippingRate;

    @Min(value = 1, message = "Max daily capacity must be at least 1")
    private Integer maxDailyCapacity;

    private LocalTime cutOffTime;
}