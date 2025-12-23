package org.project.digital_logistics.exception;

import org.project.digital_logistics.dto.ApiResponse;
import org.springframework. http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security. authentication.BadCredentialsException;
import org.springframework.security. core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org. springframework.web.bind.annotation. ExceptionHandler;
import org. springframework.web.bind.annotation. RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        ApiResponse<Void> response = new ApiResponse<>(ex. getMessage(), null);
        response.setSuccess(false);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DuplicateResourceException. class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateResource(DuplicateResourceException ex) {
        ApiResponse<Void> response = new ApiResponse<>(ex.getMessage(), null);
        response.setSuccess(false);
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientStock(InsufficientStockException ex) {
        ApiResponse<Void> response = new ApiResponse<>(ex.getMessage(), null);
        response.setSuccess(false);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidOperation(InvalidOperationException ex) {
        ApiResponse<Void> response = new ApiResponse<>(ex.getMessage(), null);
        response.setSuccess(false);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        ApiResponse<Void> response = new ApiResponse<>(ex.getMessage(), null);
        response.setSuccess(false);
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        ApiResponse<Map<String, String>> response = new ApiResponse<>("Validation failed", errors);
        response.setSuccess(false);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException. class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        ApiResponse<Void> response = new ApiResponse<>(ex.getMessage(), null);
        response.setSuccess(false);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        ApiResponse<Void> response = new ApiResponse<>(ex.getMessage(), null);
        response.setSuccess(false);
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    // ============= JWT EXCEPTIONS =============

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        ApiResponse<Void> response = new ApiResponse<>("Email ou mot de passe incorrect", null);
        response.setSuccess(false);
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UsernameNotFoundException. class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(UsernameNotFoundException ex) {
        ApiResponse<Void> response = new ApiResponse<>(ex.getMessage(), null);
        response.setSuccess(false);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex) {
        ApiResponse<Void> response = new ApiResponse<>(ex. getMessage(), null);
        response.setSuccess(false);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(Exception ex) {
        ApiResponse<Void> response = new ApiResponse<>("Une erreur interne est survenue: " + ex.getMessage(), null);
        response.setSuccess(false);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}