package org.project.digital_logistics.mapper;

import org.project.digital_logistics.dto.supplier.SupplierRequestDto;
import org.project.digital_logistics.dto.supplier.SupplierResponseDto;
import org.project.digital_logistics.model.Supplier;

public class SupplierMapper {

    private SupplierMapper(){
        throw new IllegalStateException("Utility class");
    }

    public static Supplier toEntity(SupplierRequestDto dto){
        if(dto == null){
            return null;
        }

        return Supplier.builder()
                .name(dto.getName())
                .phoneNumber(dto.getPhoneNumber())
                .address(dto.getAddress())
                .matricule(dto.getMatricule())
                .build();
    }

    public static SupplierResponseDto toResponseDto(Supplier supplier){
        if(supplier == null){
            return null;
        }

        return SupplierResponseDto.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .phoneNumber(supplier.getPhoneNumber())
                .address(supplier.getAddress())
                .matricule(supplier.getMatricule())
                .build();
    }

    public static void updateEntityFromDto(SupplierRequestDto dto, Supplier supplier){
        if(dto == null || supplier == null){
            return;
        }

        if(dto.getName() != null){
            supplier.setName(dto.getName());
        }
        if(dto.getPhoneNumber() != null){
            supplier.setPhoneNumber(dto.getPhoneNumber());
        }
        if(dto.getAddress() !=null){
            supplier.setAddress(dto.getAddress());
        }
        if(dto.getMatricule() != null){
            supplier.setMatricule(dto.getMatricule());
        }
    }
}
