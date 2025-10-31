package org.project.digital_logistics.dto.warehouse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseResponseDto {

    private Long id;
    private String name;
    private String code;
    private Integer capacity;
    private Boolean active;

    private Long managerId;
    private String managerName;
    private String managerEmail;

}