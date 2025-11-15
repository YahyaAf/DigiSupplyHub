package org.project.digital_logistics.service;

import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.product.ProductRequestDto;
import org.project.digital_logistics.dto.product.ProductResponseDto;
import org.project.digital_logistics.exception.DuplicateResourceException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.mapper.ProductMapper;
import org.project.digital_logistics.model.Product;
import org.project.digital_logistics.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final FileStorageService fileStorageService;

    @Autowired
    public ProductService(
            ProductRepository productRepository,
            ProductMapper productMapper,
            FileStorageService fileStorageService) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public ApiResponse<ProductResponseDto> createProduct(ProductRequestDto requestDto) {
        if (productRepository.existsBySku(requestDto.getSku())) {
            throw new DuplicateResourceException("Product", "sku", requestDto.getSku());
        }

        Product product = productMapper.toEntity(requestDto);
        Product savedProduct = productRepository.save(product);
        ProductResponseDto responseDto = productMapper.toResponseDto(savedProduct);

        return new ApiResponse<>("Product created successfully", responseDto);
    }

    public ApiResponse<ProductResponseDto> getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        ProductResponseDto responseDto = productMapper.toResponseDto(product);
        return new ApiResponse<>("Product retrieved successfully", responseDto);
    }

    public ApiResponse<ProductResponseDto> getProductBySku(String sku) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "sku", sku));

        ProductResponseDto responseDto = productMapper.toResponseDto(product);
        return new ApiResponse<>("Product retrieved successfully", responseDto);
    }

    public ApiResponse<List<ProductResponseDto>> getAllProducts() {
        List<ProductResponseDto> products = productRepository.findAll()
                .stream()
                .map(productMapper::toResponseDto)
                .toList();
        return new ApiResponse<>("Products retrieved successfully", products);
    }

    public ApiResponse<List<ProductResponseDto>> getProductsByCategory(String category) {
        List<ProductResponseDto> products = productRepository.findByCategory(category)
                .stream()
                .map(productMapper::toResponseDto)
                .toList();
        return new ApiResponse<>("Products retrieved successfully", products);
    }

    public ApiResponse<List<ProductResponseDto>> getActiveProducts() {
        List<ProductResponseDto> products = productRepository.findByActive(true)
                .stream()
                .map(productMapper::toResponseDto)
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

        productMapper.updateEntityFromDto(requestDto, product);
        Product savedProduct = productRepository.save(product);
        ProductResponseDto responseDto = productMapper.toResponseDto(savedProduct);

        return new ApiResponse<>("Product updated successfully", responseDto);
    }

    @Transactional
    public ApiResponse<Void> deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        if (product.getImageFilename() != null) {
            fileStorageService.deleteFile(product.getImageFilename());
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

    @Transactional
    public ApiResponse<ProductResponseDto> updateProductImage(Long id, MultipartFile imageFile) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        if (product.getImageFilename() != null) {
            fileStorageService.deleteFile(product.getImageFilename());
        }

        String filename = fileStorageService.storeFile(imageFile);
        product.setImageFilename(filename);

        Product savedProduct = productRepository.save(product);
        ProductResponseDto responseDto = productMapper.toResponseDto(savedProduct);

        return new ApiResponse<>("Product image updated successfully", responseDto);
    }

    @Transactional
    public ApiResponse<ProductResponseDto> deleteProductImage(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        if (product.getImageFilename() == null) {
            throw new IllegalStateException("Product has no image to delete");
        }

        fileStorageService.deleteFile(product.getImageFilename());

        product.setImageFilename(null);
        Product savedProduct = productRepository.save(product);

        ProductResponseDto responseDto = productMapper.toResponseDto(savedProduct);
        return new ApiResponse<>("Product image deleted successfully", responseDto);
    }
}