package org.project.digital_logistics.dto.supplier;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierRequestDto {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp ="^\\+?[0-9]{10,15}$", message = "Phone number must be valid (10-15 digits)")
    private String phoneNumber;

    @NotBlank(message ="Address is required")
    @Size(min = 5, max = 255, message ="Address must be between 5 and 55 characters")
    private String address;

    @NotBlank(message = "Matriculate is required")
    @Size(min = 2, max = 15, message = "Matriculate must be between 2 and 15 characters")
    private String matricule;





}
