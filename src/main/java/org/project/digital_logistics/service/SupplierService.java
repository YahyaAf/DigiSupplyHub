package org.project.digital_logistics.service;

import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.supplier.SupplierRequestDto;
import org.project.digital_logistics.dto.supplier.SupplierResponseDto;
import org.project.digital_logistics.exception.DuplicateResourceException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.mapper.SupplierMapper;
import org.project.digital_logistics.model.Supplier;
import org.project.digital_logistics.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SupplierService {

    private final SupplierRepository supplierRepository;

    @Autowired
    public SupplierService(SupplierRepository supplierRepository){
        this.supplierRepository = supplierRepository;
    }

    @Transactional
    public ApiResponse<SupplierResponseDto> createSupplier(SupplierRequestDto requestDto){
        if(supplierRepository.existsByMatricule(requestDto.getMatricule())){
            throw  new DuplicateResourceException("Supplier","matricule",requestDto.getMatricule());
        }
        Supplier supplier = SupplierMapper.toEntity(requestDto);
        Supplier saveSupplier = supplierRepository.save(supplier);
        SupplierResponseDto responseDto = SupplierMapper.toResponseDto(saveSupplier);

        return new ApiResponse<>("Supplier created successfully", responseDto);
    }

    public ApiResponse<SupplierResponseDto> getSupplierById(Long id){
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier","id",id));
        SupplierResponseDto responseDto = SupplierMapper.toResponseDto(supplier);
        return new ApiResponse<>("Supplier retrieved successfully",responseDto);
    }

    public ApiResponse<List<SupplierResponseDto>> getAllSuppliers(){
        List<SupplierResponseDto> suppliers = supplierRepository.findAll()
                .stream()
                .map(SupplierMapper::toResponseDto)
                .toList();
        return new ApiResponse<>("Suppliers retrieved successfully", suppliers);
    }

    @Transactional
    public ApiResponse<SupplierResponseDto> updateSupplier(Long id, SupplierRequestDto requestDto){
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier","id",id));

        if(!supplier.getMatricule().equals(requestDto.getMatricule()) &&
                supplierRepository.existsByMatricule(requestDto.getMatricule())){
            throw new DuplicateResourceException("Supplier","matricule",requestDto.getMatricule());
        }

        SupplierMapper.updateEntityFromDto(requestDto,supplier);
        Supplier saveSupplier = supplierRepository.save(supplier);
        SupplierResponseDto responseDto = SupplierMapper.toResponseDto(saveSupplier);

        return new ApiResponse<>("Supplier updated successfully",responseDto);

    }

    public ApiResponse<Void> deleteSupplier(Long id){
        if(!supplierRepository.existsById(id)){
            throw new ResourceNotFoundException("Supplier","id",id);
        }

        supplierRepository.deleteById(id);
        return new ApiResponse<>("Supplier deleted successfully", null);
    }

    public ApiResponse<Long> countSuppliers(){
        long count = supplierRepository.count();
        return new ApiResponse<>("Total suppliers counted successfully",count);
    }


}
