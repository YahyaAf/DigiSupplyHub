package org.project.digital_logistics. controller;

import io.swagger. v3.oas.annotations. tags.Tag;
import jakarta. validation.Valid;
import org.project.digital_logistics.dto.ApiResponse;
import org.project. digital_logistics.dto.product. ProductRequestDto;
import org. project.digital_logistics.dto. product.ProductResponseDto;
import org.project.digital_logistics. service.ProductService;
import org.project. digital_logistics.service.S3Service;
import org.springframework. beans.factory.annotation.Autowired;
import org.springframework. http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
@Tag(name = "Products", description = "Product Management")
public class ProductController {

    private final ProductService productService;
    private final S3Service s3Service;

    @Autowired
    public ProductController(ProductService productService, S3Service s3Service) {
        this.productService = productService;
        this.s3Service = s3Service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponseDto>> createProduct(@Valid @RequestBody ProductRequestDto requestDto) {
        ApiResponse<ProductResponseDto> response = productService.createProduct(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> getProductById(@PathVariable Long id) {
        ApiResponse<ProductResponseDto> response = productService. getProductById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> getProductBySku(@PathVariable String sku) {
        ApiResponse<ProductResponseDto> response = productService.getProductBySku(sku);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getAllProducts() {
        ApiResponse<List<ProductResponseDto>> response = productService.getAllProducts();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getProductsByCategory(@PathVariable String category) {
        ApiResponse<List<ProductResponseDto>> response = productService.getProductsByCategory(category);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getActiveProducts() {
        ApiResponse<List<ProductResponseDto>> response = productService.getActiveProducts();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDto requestDto) {
        ApiResponse<ProductResponseDto> response = productService.updateProduct(id, requestDto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        ApiResponse<Void> response = productService.deleteProduct(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> countProducts() {
        ApiResponse<Long> response = productService.countProducts();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/count/active")
    public ResponseEntity<ApiResponse<Long>> countActiveProducts() {
        ApiResponse<Long> response = productService.countActiveProducts();
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductResponseDto>> uploadProductImage(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile imageFile) {

        if (imageFile.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>("Image file is required", null));
        }

        long maxSize = 5 * 1024 * 1024;
        if (imageFile.getSize() > maxSize) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>("Image file size exceeds 5MB limit", null));
        }

        String contentType = imageFile.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest()
                    . body(new ApiResponse<>("Only image files are allowed", null));
        }

        ApiResponse<ProductResponseDto> response = productService. updateProductImage(id, imageFile);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/image")
    public ResponseEntity<ApiResponse<ProductResponseDto>> deleteProductImage(@PathVariable Long id) {
        try {
            ApiResponse<ProductResponseDto> response = productService.deleteProductImage(id);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException ex) {
            return ResponseEntity. badRequest()
                    .body(new ApiResponse<>(ex. getMessage(), null));
        }
    }

    @PostMapping(value = "/{id}/image/s3", consumes = MediaType. MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductResponseDto>> uploadProductImageS3(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile imageFile) {
        ApiResponse<ProductResponseDto> response = productService.updateProductImageS3(id, imageFile);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/S3")
    public ResponseEntity<HashMap<String, Object>> uploadS3(@RequestParam("file") MultipartFile file) {
        try {
            String url = s3Service.uploadFile(file);
            HashMap<String, Object> response = new HashMap<>();
            response. put("File Url", url);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            HashMap<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Failed to upload file to S3: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}