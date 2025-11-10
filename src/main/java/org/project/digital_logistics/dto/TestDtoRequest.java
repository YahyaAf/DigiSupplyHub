package org.project.digital_logistics.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TestDtoRequest {

    @NotNull(message = "name is required")
    private String name;

    @Email(message = "Email format is required")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String contactPhone;

    @NotNull(message = "Warehouse is required ")
    @Positive(message = "Warehouse id must be positive")
    private Long warehouseID;

    private Boolean active;


}
