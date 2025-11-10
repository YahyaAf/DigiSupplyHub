package org.project.digital_logistics.mapper;

import org.project.digital_logistics.dto.TestDtoRequest;
import org.project.digital_logistics.dto.TestDtoResponse;
import org.project.digital_logistics.model.Test;
import org.project.digital_logistics.model.Warehouse;

public class TestMapper {
    public static Test toEntity(TestDtoRequest dto, Warehouse warehouse){
        return Test.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .phoneNumber(dto.getContactPhone())
                .count(0)
                .active(dto.getActive() != null ? dto.getActive() : true)
                .warehouse(warehouse)
                .build();
    }


    public static TestDtoResponse testDtoResponse(Test test){
        return TestDtoResponse.builder()
                .id(test.getId())
                .name(test.getName())
                .email(test.getEmail())
                .phoneNumber(test.getPhoneNumber())
                .warehouseId(test.getWarehouse().getId())
                .warehouseCode(test.getWarehouse().getCode())
                .warehouseName(test.getWarehouse().getName())
                .count(test.getCount())
                .active(test.getActive())
                .build();
    }

    public static void updateEntityFromDto(TestDtoRequest dto, Test test, Warehouse warehouse){
        if(dto.getName() != null){
            test.setName(dto.getName());
        }
        if(dto.getEmail() != null){
            test.setEmail(dto.getEmail());
        }
        if(dto.getContactPhone() != null){
            test.setPhoneNumber(dto.getContactPhone());
        }
        if(dto.getActive() != null){
            test.setActive(dto.getActive());
        }
        if(warehouse != null){
            test.setWarehouse(warehouse);
        }
    }
}
