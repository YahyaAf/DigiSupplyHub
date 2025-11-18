package org.project.digital_logistics.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.project.digital_logistics.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.product.ProductRequestDto;
import org.project.digital_logistics.dto.product.ProductResponseDto;
import org.project.digital_logistics.exception.AccessDeniedException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.service.PermissionService;
import org.project.digital_logistics.service.ProductService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @MockBean
    private PermissionService permissionService;

    @MockBean
    private S3Service s3Service;

    private MockHttpSession session;
    private ProductRequestDto requestDto;
    private ProductResponseDto responseDto;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();

        // Setup ProductRequestDto
        requestDto = new ProductRequestDto();
        requestDto.setSku("PROD-001");
        requestDto.setName("Test Product");
        requestDto.setCategory("Electronics");
        requestDto.setOriginalPrice(10000L);
        requestDto.setProfite(BigDecimal.valueOf(1000));
        requestDto.setActive(true);

        // Setup ProductResponseDto
        responseDto = new ProductResponseDto();
        responseDto.setId(1L);
        responseDto.setSku("PROD-001");
        responseDto.setName("Test Product");
        responseDto.setCategory("Electronics");
        responseDto.setActive(true);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CREATE PRODUCT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void createProduct_AsAdmin_ReturnsCreated() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(productService.createProduct(any(ProductRequestDto.class)))
                .thenReturn(new ApiResponse<>("Product created successfully", responseDto));

        // When & Then
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Product created successfully"))
                .andExpect(jsonPath("$.data.sku").value("PROD-001"));

        verify(permissionService).requireAdmin(any());
        verify(productService).createProduct(any(ProductRequestDto.class));
    }

    @Test
    void createProduct_AsNonAdmin_ReturnsForbidden() throws Exception {
        // Given
        doThrow(new AccessDeniedException("Access denied"))
                .when(permissionService).requireAdmin(any());

        // When & Then
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isForbidden());

        verify(productService, never()).createProduct(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // GET PRODUCT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getProductById_Success_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(productService.getProductById(1L))
                .thenReturn(new ApiResponse<>("Product retrieved successfully", responseDto));

        // When & Then
        mockMvc.perform(get("/api/products/1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.sku").value("PROD-001"));

        verify(productService).getProductById(1L);
    }

    @Test
    void getProductById_NotFound_ReturnsNotFound() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(productService.getProductById(999L))
                .thenThrow(new ResourceNotFoundException("Product", "id", 999L));

        // When & Then
        mockMvc.perform(get("/api/products/999")
                        .session(session))
                .andExpect(status().isNotFound());

        verify(productService).getProductById(999L);
    }

    @Test
    void getProductBySku_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(productService.getProductBySku("PROD-001"))
                .thenReturn(new ApiResponse<>("Product retrieved", responseDto));

        // When & Then
        mockMvc.perform(get("/api/products/sku/PROD-001")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sku").value("PROD-001"));

        verify(productService).getProductBySku("PROD-001");
    }

    @Test
    void getAllProducts_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(productService.getAllProducts())
                .thenReturn(new ApiResponse<>("Products retrieved", Arrays.asList(responseDto)));

        // When & Then
        mockMvc.perform(get("/api/products")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].sku").value("PROD-001"));

        verify(productService).getAllProducts();
    }

    @Test
    void getProductsByCategory_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(productService.getProductsByCategory("Electronics"))
                .thenReturn(new ApiResponse<>("Products retrieved", Arrays.asList(responseDto)));

        // When & Then
        mockMvc.perform(get("/api/products/category/Electronics")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        verify(productService).getProductsByCategory("Electronics");
    }

    @Test
    void getActiveProducts_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(productService.getActiveProducts())
                .thenReturn(new ApiResponse<>("Active products retrieved", Arrays.asList(responseDto)));

        // When & Then
        mockMvc.perform(get("/api/products/active")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        verify(productService).getActiveProducts();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // UPDATE PRODUCT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void updateProduct_AsAdmin_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(productService.updateProduct(eq(1L), any(ProductRequestDto.class)))
                .thenReturn(new ApiResponse<>("Product updated", responseDto));

        // When & Then
        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Product updated"));

        verify(productService).updateProduct(eq(1L), any(ProductRequestDto.class));
    }

    @Test
    void updateProduct_AsNonAdmin_ReturnsForbidden() throws Exception {
        // Given
        doThrow(new AccessDeniedException("Access denied"))
                .when(permissionService).requireAdmin(any());

        // When & Then
        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .session(session))
                .andExpect(status().isForbidden());

        verify(productService, never()).updateProduct(anyLong(), any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // DELETE PRODUCT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void deleteProduct_AsAdmin_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(productService.deleteProduct(1L))
                .thenReturn(new ApiResponse<>("Product deleted", null));

        // When & Then
        mockMvc.perform(delete("/api/products/1")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Product deleted"));

        verify(productService).deleteProduct(1L);
    }

    @Test
    void deleteProduct_AsNonAdmin_ReturnsForbidden() throws Exception {
        // Given
        doThrow(new AccessDeniedException("Access denied"))
                .when(permissionService).requireAdmin(any());

        // When & Then
        mockMvc.perform(delete("/api/products/1")
                        .session(session))
                .andExpect(status().isForbidden());

        verify(productService, never()).deleteProduct(anyLong());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // COUNT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void countProducts_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(productService.countProducts())
                .thenReturn(new ApiResponse<>("Total products", 50L));

        // When & Then
        mockMvc.perform(get("/api/products/count")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(50));

        verify(productService).countProducts();
    }

    @Test
    void countActiveProducts_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(productService.countActiveProducts())
                .thenReturn(new ApiResponse<>("Active products count", 30L));

        // When & Then
        mockMvc.perform(get("/api/products/count/active")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(30));

        verify(productService).countActiveProducts();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // IMAGE UPLOAD TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void uploadProductImage_ValidImage_ReturnsOk() throws Exception {
        // Given
        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "product.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        doNothing().when(permissionService).requireAdmin(any());
        when(productService.updateProductImage(eq(1L), any()))
                .thenReturn(new ApiResponse<>("Image uploaded", responseDto));

        // When & Then
        mockMvc.perform(multipart("/api/products/1/image")
                        .file(imageFile)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Image uploaded"));

        verify(productService).updateProductImage(eq(1L), any());
    }

    @Test
    void uploadProductImage_EmptyFile_ReturnsBadRequest() throws Exception {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "image",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        doNothing().when(permissionService).requireAdmin(any());

        // When & Then
        mockMvc.perform(multipart("/api/products/1/image")
                        .file(emptyFile)
                        .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Image file is required"));

        verify(productService, never()).updateProductImage(anyLong(), any());
    }

    @Test
    void uploadProductImage_FileTooLarge_ReturnsBadRequest() throws Exception {
        // Given
        byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "image",
                "large.jpg",
                "image/jpeg",
                largeContent
        );

        doNothing().when(permissionService).requireAdmin(any());

        // When & Then
        mockMvc.perform(multipart("/api/products/1/image")
                        .file(largeFile)
                        .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Image file size exceeds 5MB limit"));

        verify(productService, never()).updateProductImage(anyLong(), any());
    }

    @Test
    void uploadProductImage_InvalidFileType_ReturnsBadRequest() throws Exception {
        // Given
        MockMultipartFile pdfFile = new MockMultipartFile(
                "image",
                "document.pdf",
                "application/pdf",
                "pdf content".getBytes()
        );

        doNothing().when(permissionService).requireAdmin(any());

        // When & Then
        mockMvc.perform(multipart("/api/products/1/image")
                        .file(pdfFile)
                        .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only image files are allowed"));

        verify(productService, never()).updateProductImage(anyLong(), any());
    }

    @Test
    void uploadProductImage_AsNonAdmin_ReturnsForbidden() throws Exception {
        // Given
        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "product.jpg",
                "image/jpeg",
                "test content".getBytes()
        );

        doThrow(new AccessDeniedException("Access denied"))
                .when(permissionService).requireAdmin(any());

        // When & Then
        mockMvc.perform(multipart("/api/products/1/image")
                        .file(imageFile)
                        .session(session))
                .andExpect(status().isForbidden());

        verify(productService, never()).updateProductImage(anyLong(), any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // DELETE IMAGE TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void deleteProductImage_Success_ReturnsOk() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(productService.deleteProductImage(1L))
                .thenReturn(new ApiResponse<>("Image deleted", responseDto));

        // When & Then
        mockMvc.perform(delete("/api/products/1/image")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Image deleted"));

        verify(productService).deleteProductImage(1L);
    }

    @Test
    void deleteProductImage_NoImage_ReturnsBadRequest() throws Exception {
        // Given
        doNothing().when(permissionService).requireAdmin(any());
        when(productService.deleteProductImage(1L))
                .thenThrow(new IllegalStateException("Product has no image to delete"));

        // When & Then
        mockMvc.perform(delete("/api/products/1/image")
                        .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Product has no image to delete"));

        verify(productService).deleteProductImage(1L);
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// S3 IMAGE UPLOAD TESTS - VERSION CORRIGÉE
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void uploadProductImageS3_ValidImage_ReturnsOk() throws Exception {
        // Given
        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "product-s3.jpg",
                "image/jpeg",
                "test s3 image content".getBytes()
        );

        ProductResponseDto s3ResponseDto = ProductResponseDto.builder()
                .id(1L)
                .sku("PROD-001")
                .name("Test Product")
                .imageS3Url("https://s3.amazonaws.com/bucket/product-s3.jpg")
                .build();

        ApiResponse<ProductResponseDto> apiResponse = new ApiResponse<>(
                "Product image uploaded to S3 successfully",
                s3ResponseDto
        );

        doNothing().when(permissionService).requireAdmin(any());
        when(productService.updateProductImageS3(eq(1L), any(MultipartFile.class)))
                .thenReturn(apiResponse);

        // When & Then
        mockMvc.perform(multipart("/api/products/{id}/image/s3", 1L)
                        .file(imageFile)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Product image uploaded to S3 successfully"))
                .andExpect(jsonPath("$.data.imageS3Url").value("https://s3.amazonaws.com/bucket/product-s3.jpg"));

        verify(permissionService).requireAdmin(any());
        verify(productService).updateProductImageS3(eq(1L), any(MultipartFile.class));
    }

    @Test
    void uploadProductImageS3_ProductNotFound_ReturnsNotFound() throws Exception {
        // Given
        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "product-s3.jpg",
                "image/jpeg",
                "test content".getBytes()
        );

        doNothing().when(permissionService).requireAdmin(any());
        when(productService.updateProductImageS3(eq(999L), any(MultipartFile.class)))
                .thenThrow(new ResourceNotFoundException("Product", "id", 999L));

        // When & Then
        mockMvc.perform(multipart("/api/products/{id}/image/s3", 999L)
                        .file(imageFile)
                        .session(session))
                .andExpect(status().isNotFound());

        verify(permissionService).requireAdmin(any());
        verify(productService).updateProductImageS3(eq(999L), any(MultipartFile.class));
    }

// SUPPRIMEZ ces tests car la validation n'existe pas dans votre controller :
// - uploadProductImageS3_EmptyFile_ReturnsBadRequest()
// - uploadProductImageS3_FileTooLarge_ReturnsBadRequest()
// - uploadProductImageS3_InvalidFileType_ReturnsBadRequest()

    @Test
    void uploadProductImageS3_AsNonAdmin_ReturnsForbidden() throws Exception {
        // Given
        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "product-s3.jpg",
                "image/jpeg",
                "test content".getBytes()
        );

        doThrow(new AccessDeniedException("Access denied"))
                .when(permissionService).requireAdmin(any());

        // When & Then
        mockMvc.perform(multipart("/api/products/{id}/image/s3", 1L)
                        .file(imageFile)
                        .session(session))
                .andExpect(status().isForbidden());

        verify(productService, never()).updateProductImageS3(anyLong(), any());
    }

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// DIRECT S3 UPLOAD TESTS - VERSION CORRIGÉE
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void uploadS3_ValidFile_ReturnsOk() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "test file content".getBytes()
        );

        String s3Url = "https://s3.amazonaws.com/bucket/document.pdf";
        when(s3Service.uploadFile(file)).thenReturn(s3Url);

        // When & Then
        mockMvc.perform(multipart("/api/products/S3")
                        .file(file)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['File Url']").value(s3Url)); // ✅ CORRECTION ICI

        verify(s3Service).uploadFile(file);
    }

// SUPPRIMEZ ces tests car la validation n'existe pas :
// - uploadS3_EmptyFile_ReturnsBadRequest()
// - uploadS3_FileTooLarge_ReturnsBadRequest()

    @Test
    void uploadS3_S3ServiceError_ReturnsInternalServerError() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        when(s3Service.uploadFile(file))
                .thenThrow(new RuntimeException("S3 service unavailable"));

        // When & Then
        mockMvc.perform(multipart("/api/products/S3")
                        .file(file)
                        .session(session))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Failed to upload file to S3: S3 service unavailable"));

        verify(s3Service).uploadFile(file);
    }
}