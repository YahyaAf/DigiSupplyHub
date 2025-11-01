package org.project.digital_logistics.mapper;

import org.project.digital_logistics.dto.product.ProductRequestDto;
import org.project.digital_logistics.dto.product.ProductResponseDto;
import org.project.digital_logistics.model.Product;

import java.math.BigDecimal;

public class ProductMapper {

    private ProductMapper() {
        throw new IllegalStateException("Utility class");
    }

    public static Product toEntity(ProductRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return Product.builder()
                .sku(dto.getSku())
                .name(dto.getName())
                .category(dto.getCategory())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .originalPrice(dto.getOriginalPrice())
                .profite(dto.getProfite())
                .build();
    }

    public static ProductResponseDto toResponseDto(Product product) {
        if (product == null) {
            return null;
        }

        BigDecimal sellingPrice = BigDecimal.valueOf(product.getOriginalPrice())
                .add(product.getProfite());

        return ProductResponseDto.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .category(product.getCategory())
                .active(product.getActive())
                .originalPrice(product.getOriginalPrice())
                .profite(product.getProfite())
                .sellingPrice(sellingPrice)
                .build();
    }

    public static void updateEntityFromDto(ProductRequestDto dto, Product product) {
        if (dto == null || product == null) {
            return;
        }

        if (dto.getSku() != null) {
            product.setSku(dto.getSku());
        }
        if (dto.getName() != null) {
            product.setName(dto.getName());
        }
        if (dto.getCategory() != null) {
            product.setCategory(dto.getCategory());
        }
        if (dto.getActive() != null) {
            product.setActive(dto.getActive());
        }
        if (dto.getOriginalPrice() != null) {
            product.setOriginalPrice(dto.getOriginalPrice());
        }
        if (dto.getProfite() != null) {
            product.setProfite(dto.getProfite());
        }
    }
}