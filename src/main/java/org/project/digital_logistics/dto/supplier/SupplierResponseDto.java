package org.project.digital_logistics.dto.supplier;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierResponseDto {
    private Long id;
    private String name;
    private String phoneNumber;
    private String address;
    private String matricule;
}
