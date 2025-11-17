package org.project.digital_logistics.mapper;

import org.project.digital_logistics.dto.product.ProductRequestDto;
import org.project.digital_logistics.dto.product.ProductResponseDto;
import org.project.digital_logistics.model.Product;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ProductMapper {

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${server.address:localhost}")
    private String serverAddress;

    public Product toEntity(ProductRequestDto dto) {
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

    public ProductResponseDto toResponseDto(Product product) {
        if (product == null) {
            return null;
        }

        String imageUrl = null;
        if (product.getImageFilename() != null) {
            imageUrl = String.format("http://%s:%s/api/images/%s",
                    serverAddress, serverPort, product.getImageFilename());
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
                .imageUrl(imageUrl)
                .imageS3Url(product.getImageS3Url())
                .sellingPrice(sellingPrice)
                .build();
    }

    public void updateEntityFromDto(ProductRequestDto dto, Product product) {
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