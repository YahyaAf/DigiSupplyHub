package org.project.digital_logistics.mapper;

import org.project.digital_logistics.dto.warehouse.WarehouseRequestDto;
import org.project.digital_logistics.dto.warehouse.WarehouseResponseDto;
import org.project.digital_logistics.model.User;
import org.project.digital_logistics.model.Warehouse;

public class WarehouseMapper {

    private WarehouseMapper() {
        throw new IllegalStateException("Utility class");
    }

    public static Warehouse toEntity(WarehouseRequestDto dto, User manager) {
        if (dto == null) {
            return null;
        }

        return Warehouse.builder()
                .name(dto.getName())
                .code(dto.getCode())
                .capacity(dto.getCapacity())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .manager(manager)
                .build();
    }

    public static WarehouseResponseDto toResponseDto(Warehouse warehouse) {
        if (warehouse == null) {
            return null;
        }

        return WarehouseResponseDto.builder()
                .id(warehouse.getId())
                .name(warehouse.getName())
                .code(warehouse.getCode())
                .capacity(warehouse.getCapacity())
                .active(warehouse.getActive())
                .managerId(warehouse.getManager() != null ? warehouse.getManager().getId() : null)
                .managerName(warehouse.getManager() != null ? warehouse.getManager().getName() : null)
                .managerEmail(warehouse.getManager() != null ? warehouse.getManager().getEmail() : null)
                .build();
    }

    public static void updateEntityFromDto(WarehouseRequestDto dto, Warehouse warehouse, User manager) {
        if (dto == null || warehouse == null) {
            return;
        }

        if (dto.getName() != null) {
            warehouse.setName(dto.getName());
        }
        if (dto.getCode() != null) {
            warehouse.setCode(dto.getCode());
        }
        if (dto.getCapacity() != null) {
            warehouse.setCapacity(dto.getCapacity());
        }
        if (dto.getActive() != null) {
            warehouse.setActive(dto.getActive());
        }
        if (manager != null) {
            warehouse.setManager(manager);
        }
    }
}