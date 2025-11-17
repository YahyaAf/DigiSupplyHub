package org.project.digital_logistics.dto.product;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequestDto {

    @NotBlank(message = "SKU is required")
    @Size(min = 2, max = 50, message = "SKU must be between 2 and 50 characters")
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "SKU must contain only uppercase letters, numbers, and hyphens")
    private String sku;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 200, message = "Name must be between 2 and 200 characters")
    private String name;

    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    @Builder.Default
    private Boolean active = true;

    @NotNull(message = "Original price is required")
    @Min(value = 0, message = "Original price must be positive")
    private Long originalPrice;

    @NotNull(message = "Profit is required")
    @DecimalMin(value = "0.00", message = "Profit must be positive")
    @Digits(integer = 17, fraction = 2, message = "Profit must have max 2 decimal places")
    private BigDecimal profite;

    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    @Pattern(
            regexp = "^(https?://.*\\.(jpg|jpeg|png|gif|webp|svg))?$",
            message = "Image must be a valid URL ending with jpg, jpeg, png, gif, webp, or svg"
    )
    private String image;
    private String imageS3;
}