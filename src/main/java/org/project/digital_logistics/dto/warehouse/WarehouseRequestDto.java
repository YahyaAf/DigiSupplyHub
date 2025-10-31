package org.project.digital_logistics.dto.warehouse;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseRequestDto {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Code is required")
    @Size(min = 2, max = 20, message = "Code must be between 2 and 20 characters")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Code must contain only uppercase letters and numbers")
    private String code;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 100000, message = "Capacity cannot exceed 100000")
    private Integer capacity;

    @Builder.Default
    private Boolean active = true;

    @NotNull(message = "Manager ID is required")
    @Positive(message = "Manager ID must be a positive number")
    private Long managerId;

}