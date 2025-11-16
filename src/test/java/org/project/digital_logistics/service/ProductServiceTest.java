package org.project.digital_logistics.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.product.ProductRequestDto;
import org.project.digital_logistics.dto.product.ProductResponseDto;
import org.project.digital_logistics.exception.DuplicateResourceException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.mapper.ProductMapper;
import org.project.digital_logistics.model.Product;
import org.project.digital_logistics.repository.ProductRepository;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private ProductRequestDto requestDto;
    private ProductResponseDto responseDto;

    @BeforeEach
    void setUp() {
        // Setup ProductRequestDto
        requestDto = new ProductRequestDto();
        requestDto.setSku("PROD-001");
        requestDto.setName("Test Product");
        requestDto.setCategory("Electronics");
        requestDto.setOriginalPrice(10000L);
        requestDto.setProfite(BigDecimal.valueOf(1000));
        requestDto.setActive(true);

        // Setup Product Entity
        product = Product.builder()
                .id(1L)
                .sku("PROD-001")
                .name("Test Product")
                .category("Electronics")
                .originalPrice(10000L)
                .profite(BigDecimal.valueOf(1000))
                .imageFilename(null)
                .active(true)
                .build();

        // Setup ProductResponseDto
        responseDto = new ProductResponseDto();
        responseDto.setId(1L);
        responseDto.setSku("PROD-001");
        responseDto.setName("Test Product");
        responseDto.setCategory("Electronics");
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CREATE PRODUCT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void createProduct_Success() {
        // Given
        when(productRepository.existsBySku("PROD-001")).thenReturn(false);
        when(productMapper.toEntity(requestDto)).thenReturn(product);
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponseDto(product)).thenReturn(responseDto);

        // When
        ApiResponse<ProductResponseDto> response = productService.createProduct(requestDto);

        // Then
        assertNotNull(response);
        assertEquals("Product created successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals("PROD-001", response.getData().getSku());

        verify(productRepository).existsBySku("PROD-001");
        verify(productMapper).toEntity(requestDto);
        verify(productRepository).save(product);
        verify(productMapper).toResponseDto(product);
    }

    @Test
    void createProduct_DuplicateSku_ThrowsException() {
        // Given
        when(productRepository.existsBySku("PROD-001")).thenReturn(true);

        // When & Then
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> productService.createProduct(requestDto)
        );

        assertTrue(exception.getMessage().contains("sku"));
        assertTrue(exception.getMessage().contains("PROD-001"));

        verify(productRepository).existsBySku("PROD-001");
        verify(productMapper, never()).toEntity(any());
        verify(productRepository, never()).save(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // GET PRODUCT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getProductById_Success() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productMapper.toResponseDto(product)).thenReturn(responseDto);

        // When
        ApiResponse<ProductResponseDto> response = productService.getProductById(1L);

        // Then
        assertNotNull(response);
        assertEquals("Product retrieved successfully", response.getMessage());
        assertEquals(1L, response.getData().getId());

        verify(productRepository).findById(1L);
        verify(productMapper).toResponseDto(product);
    }

    @Test
    void getProductById_NotFound_ThrowsException() {
        // Given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> productService.getProductById(999L)
        );

        assertTrue(exception.getMessage().contains("Product"));
        assertTrue(exception.getMessage().contains("id"));

        verify(productRepository).findById(999L);
        verify(productMapper, never()).toResponseDto(any());
    }

    @Test
    void getProductBySku_Success() {
        // Given
        when(productRepository.findBySku("PROD-001")).thenReturn(Optional.of(product));
        when(productMapper.toResponseDto(product)).thenReturn(responseDto);

        // When
        ApiResponse<ProductResponseDto> response = productService.getProductBySku("PROD-001");

        // Then
        assertNotNull(response);
        assertEquals("Product retrieved successfully", response.getMessage());
        assertEquals("PROD-001", response.getData().getSku());

        verify(productRepository).findBySku("PROD-001");
        verify(productMapper).toResponseDto(product);
    }

    @Test
    void getProductBySku_NotFound_ThrowsException() {
        // Given
        when(productRepository.findBySku("INVALID")).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> productService.getProductBySku("INVALID")
        );

        assertTrue(exception.getMessage().contains("Product"));
        assertTrue(exception.getMessage().contains("sku"));

        verify(productRepository).findBySku("INVALID");
    }

    @Test
    void getAllProducts_Success() {
        // Given
        List<Product> products = Arrays.asList(product);
        when(productRepository.findAll()).thenReturn(products);
        when(productMapper.toResponseDto(product)).thenReturn(responseDto);

        // When
        ApiResponse<List<ProductResponseDto>> response = productService.getAllProducts();

        // Then
        assertNotNull(response);
        assertEquals("Products retrieved successfully", response.getMessage());
        assertEquals(1, response.getData().size());

        verify(productRepository).findAll();
        verify(productMapper).toResponseDto(product);
    }

    @Test
    void getProductsByCategory_Success() {
        // Given
        List<Product> products = Arrays.asList(product);
        when(productRepository.findByCategory("Electronics")).thenReturn(products);
        when(productMapper.toResponseDto(product)).thenReturn(responseDto);

        // When
        ApiResponse<List<ProductResponseDto>> response =
                productService.getProductsByCategory("Electronics");

        // Then
        assertNotNull(response);
        assertEquals("Products retrieved successfully", response.getMessage());
        assertEquals(1, response.getData().size());

        verify(productRepository).findByCategory("Electronics");
    }

    @Test
    void getActiveProducts_Success() {
        // Given
        List<Product> products = Arrays.asList(product);
        when(productRepository.findByActive(true)).thenReturn(products);
        when(productMapper.toResponseDto(product)).thenReturn(responseDto);

        // When
        ApiResponse<List<ProductResponseDto>> response = productService.getActiveProducts();

        // Then
        assertNotNull(response);
        assertEquals("Active products retrieved successfully", response.getMessage());
        assertEquals(1, response.getData().size());

        verify(productRepository).findByActive(true);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // UPDATE PRODUCT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void updateProduct_Success() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        doNothing().when(productMapper).updateEntityFromDto(requestDto, product);
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponseDto(product)).thenReturn(responseDto);

        // When
        ApiResponse<ProductResponseDto> response = productService.updateProduct(1L, requestDto);

        // Then
        assertNotNull(response);
        assertEquals("Product updated successfully", response.getMessage());

        verify(productRepository).findById(1L);
        verify(productMapper).updateEntityFromDto(requestDto, product);
        verify(productRepository).save(product);
    }

    @Test
    void updateProduct_NotFound_ThrowsException() {
        // Given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> productService.updateProduct(999L, requestDto)
        );

        assertTrue(exception.getMessage().contains("Product"));

        verify(productRepository).findById(999L);
        verify(productRepository, never()).save(any());
    }

    @Test
    void updateProduct_DuplicateSku_ThrowsException() {
        // Given
        requestDto.setSku("PROD-002"); // Different SKU
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.existsBySku("PROD-002")).thenReturn(true);

        // When & Then
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> productService.updateProduct(1L, requestDto)
        );

        assertTrue(exception.getMessage().contains("sku"));

        verify(productRepository).findById(1L);
        verify(productRepository).existsBySku("PROD-002");
        verify(productRepository, never()).save(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // DELETE PRODUCT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void deleteProduct_WithoutImage_Success() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        doNothing().when(productRepository).deleteById(1L);

        // When
        ApiResponse<Void> response = productService.deleteProduct(1L);

        // Then
        assertNotNull(response);
        assertEquals("Product deleted successfully", response.getMessage());

        verify(productRepository).findById(1L);
        verify(fileStorageService, never()).deleteFile(anyString());
        verify(productRepository).deleteById(1L);
    }

    @Test
    void deleteProduct_WithImage_DeletesImageAndProduct() {
        // Given
        product.setImageFilename("test-image.jpg");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        doNothing().when(fileStorageService).deleteFile("test-image.jpg");
        doNothing().when(productRepository).deleteById(1L);

        // When
        ApiResponse<Void> response = productService.deleteProduct(1L);

        // Then
        assertNotNull(response);
        assertEquals("Product deleted successfully", response.getMessage());

        verify(productRepository).findById(1L);
        verify(fileStorageService).deleteFile("test-image.jpg");
        verify(productRepository).deleteById(1L);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // COUNT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void countProducts_Success() {
        // Given
        when(productRepository.count()).thenReturn(50L);

        // When
        ApiResponse<Long> response = productService.countProducts();

        // Then
        assertNotNull(response);
        assertEquals("Total products counted successfully", response.getMessage());
        assertEquals(50L, response.getData());

        verify(productRepository).count();
    }

    @Test
    void countActiveProducts_Success() {
        // Given
        when(productRepository.countByActive(true)).thenReturn(30L);

        // When
        ApiResponse<Long> response = productService.countActiveProducts();

        // Then
        assertNotNull(response);
        assertEquals("Active products counted successfully", response.getMessage());
        assertEquals(30L, response.getData());

        verify(productRepository).countByActive(true);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // IMAGE MANAGEMENT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void updateProductImage_Success() {
        // Given
        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "new-image.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(fileStorageService.storeFile(imageFile)).thenReturn("stored-image.jpg");
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponseDto(product)).thenReturn(responseDto);

        // When
        ApiResponse<ProductResponseDto> response =
                productService.updateProductImage(1L, imageFile);

        // Then
        assertNotNull(response);
        assertEquals("Product image updated successfully", response.getMessage());

        verify(productRepository).findById(1L);
        verify(fileStorageService).storeFile(imageFile);
        verify(productRepository).save(product);
        verify(fileStorageService, never()).deleteFile(anyString());
    }

    @Test
    void updateProductImage_ReplacesExistingImage() {
        // Given
        product.setImageFilename("old-image.jpg");
        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "new-image.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        doNothing().when(fileStorageService).deleteFile("old-image.jpg");
        when(fileStorageService.storeFile(imageFile)).thenReturn("new-stored-image.jpg");
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponseDto(product)).thenReturn(responseDto);

        // When
        ApiResponse<ProductResponseDto> response =
                productService.updateProductImage(1L, imageFile);

        // Then
        assertNotNull(response);
        assertEquals("Product image updated successfully", response.getMessage());

        verify(productRepository).findById(1L);
        verify(fileStorageService).deleteFile("old-image.jpg");
        verify(fileStorageService).storeFile(imageFile);
        verify(productRepository).save(product);
    }

    @Test
    void deleteProductImage_Success() {
        // Given
        product.setImageFilename("test-image.jpg");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        doNothing().when(fileStorageService).deleteFile("test-image.jpg");
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponseDto(product)).thenReturn(responseDto);

        // When
        ApiResponse<ProductResponseDto> response = productService.deleteProductImage(1L);

        // Then
        assertNotNull(response);
        assertEquals("Product image deleted successfully", response.getMessage());

        verify(productRepository).findById(1L);
        verify(fileStorageService).deleteFile("test-image.jpg");
        verify(productRepository).save(product);
        assertNull(product.getImageFilename());
    }

    @Test
    void deleteProductImage_NoImage_ThrowsException() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> productService.deleteProductImage(1L)
        );

        assertEquals("Product has no image to delete", exception.getMessage());

        verify(productRepository).findById(1L);
        verify(fileStorageService, never()).deleteFile(anyString());
        verify(productRepository, never()).save(any());
    }
}