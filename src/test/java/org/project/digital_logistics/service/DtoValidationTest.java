package org.project.digital_logistics.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.project.digital_logistics.dto.product.ProductRequestDto;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test: La validation des DTO - ProductRequestDto
 *
 * Vérifie:
 * - @NotBlank, @NotNull
 * - @Size, @Pattern
 * - @Min, @DecimalMin
 * - Messages d'erreur personnalisés
 */
class DtoValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testProductDto_AllFieldsValid() {
        // Given: Tous les champs valides
        ProductRequestDto dto = ProductRequestDto.builder()
                .sku("PROD-001")
                .name("Dell Laptop")
                .category("Electronics")
                .active(true)
                .originalPrice(15000L)
                .profite(BigDecimal.valueOf(1500.50))
                .build();

        // When
        Set<ConstraintViolation<ProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.isEmpty(),
                "DTO valide ne doit pas avoir de violations");
    }

    @Test
    void testProductDto_SkuBlank() {
        // Given: SKU vide
        ProductRequestDto dto = ProductRequestDto.builder()
                .sku("")
                .name("Dell Laptop")
                .originalPrice(15000L)
                .profite(BigDecimal.valueOf(1500.00))
                .build();

        // When
        Set<ConstraintViolation<ProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertFalse(violations.isEmpty(), "SKU vide doit générer une violation");

        String message = violations.iterator().next().getMessage();
        assertEquals("SKU is required", message);
    }

    @Test
    void testProductDto_SkuInvalidPattern() {
        // Given: SKU avec lowercase (invalide)
        ProductRequestDto dto = ProductRequestDto.builder()
                .sku("prod-001")
                .name("Dell Laptop")
                .originalPrice(15000L)
                .profite(BigDecimal.valueOf(1500.00))
                .build();

        // When
        Set<ConstraintViolation<ProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertFalse(violations.isEmpty(), "SKU lowercase doit générer une violation");

        String message = violations.iterator().next().getMessage();
        assertEquals("SKU must contain only uppercase letters, numbers, and hyphens", message);
    }

    @Test
    void testProductDto_OriginalPriceNull() {
        // Given: originalPrice null
        ProductRequestDto dto = ProductRequestDto.builder()
                .sku("PROD-001")
                .name("Dell Laptop")
                .originalPrice(null)
                .profite(BigDecimal.valueOf(1500.00))
                .build();

        // When
        Set<ConstraintViolation<ProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertFalse(violations.isEmpty(), "originalPrice null doit générer une violation");

        String message = violations.iterator().next().getMessage();
        assertEquals("Original price is required", message);
    }

    @Test
    void testProductDto_ProfiteNegative() {
        // Given: profite négatif
        ProductRequestDto dto = ProductRequestDto.builder()
                .sku("PROD-001")
                .name("Dell Laptop")
                .originalPrice(15000L)
                .profite(BigDecimal.valueOf(-100.00))
                .build();

        // When
        Set<ConstraintViolation<ProductRequestDto>> violations = validator.validate(dto);

        // Then
        assertFalse(violations.isEmpty(), "Profite négatif doit générer une violation");

        String message = violations.iterator().next().getMessage();
        assertEquals("Profit must be positive", message);
    }
}