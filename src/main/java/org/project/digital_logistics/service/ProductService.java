package org.project.digital_logistics.service;

import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.product.ProductRequestDto;
import org.project.digital_logistics.dto.product.ProductResponseDto;
import org.project.digital_logistics.exception.DuplicateResourceException;
import org.project.digital_logistics.exception.InvalidOperationException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.mapper.ProductMapper;
import org.project.digital_logistics.model.Product;
import org.project.digital_logistics.model.SalesOrderLine;
import org.project.digital_logistics.repository.ProductRepository;
import org.project.digital_logistics.repository.SalesOrderLineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final SalesOrderLineRepository salesOrderLineRepository;

    @Autowired
    public ProductService(ProductRepository productRepository,SalesOrderLineRepository salesOrderLineRepository) {
        this.productRepository = productRepository;
        this.salesOrderLineRepository = salesOrderLineRepository;
    }

    @Transactional
    public ApiResponse<ProductResponseDto> createProduct(ProductRequestDto requestDto) {
        // Check if SKU already exists
        if (productRepository.existsBySku(requestDto.getSku())) {
            throw new DuplicateResourceException("Product", "sku", requestDto.getSku());
        }

        Product product = ProductMapper.toEntity(requestDto);
        Product savedProduct = productRepository.save(product);
        ProductResponseDto responseDto = ProductMapper.toResponseDto(savedProduct);

        return new ApiResponse<>("Product created successfully", responseDto);
    }

    public ApiResponse<ProductResponseDto> getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        ProductResponseDto responseDto = ProductMapper.toResponseDto(product);
        return new ApiResponse<>("Product retrieved successfully", responseDto);
    }

    public ApiResponse<ProductResponseDto> getProductBySku(String sku) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "sku", sku));

        ProductResponseDto responseDto = ProductMapper.toResponseDto(product);
        return new ApiResponse<>("Product retrieved successfully", responseDto);
    }

    public ApiResponse<List<ProductResponseDto>> getAllProducts() {
        List<ProductResponseDto> products = productRepository.findAll()
                .stream()
                .map(ProductMapper::toResponseDto)
                .toList();
        return new ApiResponse<>("Products retrieved successfully", products);
    }

    public ApiResponse<List<ProductResponseDto>> getProductsByCategory(String category) {
        List<ProductResponseDto> products = productRepository.findByCategory(category)
                .stream()
                .map(ProductMapper::toResponseDto)
                .toList();
        return new ApiResponse<>("Products retrieved successfully", products);
    }

    public ApiResponse<List<ProductResponseDto>> getActiveProducts() {
        List<ProductResponseDto> products = productRepository.findByActive(true)
                .stream()
                .map(ProductMapper::toResponseDto)
                .toList();
        return new ApiResponse<>("Active products retrieved successfully", products);
    }

    @Transactional
    public ApiResponse<ProductResponseDto> updateProduct(Long id, ProductRequestDto requestDto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        if (!product.getSku().equals(requestDto.getSku()) &&
                productRepository.existsBySku(requestDto.getSku())) {
            throw new DuplicateResourceException("Product", "sku", requestDto.getSku());
        }

        ProductMapper.updateEntityFromDto(requestDto, product);
        Product savedProduct = productRepository.save(product);
        ProductResponseDto responseDto = ProductMapper.toResponseDto(savedProduct);

        return new ApiResponse<>("Product updated successfully", responseDto);
    }

    @Transactional
    public ApiResponse<Void> deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product", "id", id);
        }

        productRepository.deleteById(id);
        return new ApiResponse<>("Product deleted successfully", null);
    }

    public ApiResponse<Long> countProducts() {
        long count = productRepository.count();
        return new ApiResponse<>("Total products counted successfully", count);
    }

    public ApiResponse<Long> countActiveProducts() {
        long count = productRepository.countByActive(true);
        return new ApiResponse<>("Active products counted successfully", count);
    }

    //ajouter desactivate product
    public ApiResponse<ProductResponseDto> desactivateProduct(String sku){
        if(!productRepository.existsBySku(sku)){
            throw new ResourceNotFoundException("Product", "id", sku);
        }
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", sku));

        if(salesOrderLineRepository.existsByProduct(product)){
            throw new InvalidOperationException("Product already reserved");
        }
        product.setActive(false);
        Product saveProduct = productRepository.save(product);
        ProductResponseDto responseDto = ProductMapper.toResponseDto(saveProduct);

        return new ApiResponse<>("Desactivate product avec success ",responseDto);
    }
}