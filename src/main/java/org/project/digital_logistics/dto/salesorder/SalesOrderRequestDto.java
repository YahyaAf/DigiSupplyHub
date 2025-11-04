package org.project.digital_logistics.dto.salesorder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesOrderRequestDto {

    @NotNull(message = "Client ID is required")
    @Positive(message = "Client ID must be positive")
    private Long clientId;

    @NotNull(message = "Warehouse ID is required")
    @Positive(message = "Warehouse ID must be positive")
    private Long warehouseId;

    @NotNull(message = "Order lines are required")
    @NotEmpty(message = "Order must have at least one line")
    @Valid
    private List<SalesOrderLineDto> orderLines;
}