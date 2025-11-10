package org.project.digital_logistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TestDtoResponse {

    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private Integer count;

    private Long warehouseId;
    private String warehouseName;
    private String warehouseCode;

    private Boolean active;

}
