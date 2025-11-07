package org.project.digital_logistics.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.UserRequestDto;
import org.project.digital_logistics.dto.UserResponseDto;
import org.project.digital_logistics.service.PermissionService;
import org.project.digital_logistics.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private final PermissionService permissionService;

    @Autowired
    public UserController(UserService userService, PermissionService permissionService) {
        this.userService = userService;
        this.permissionService = permissionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponseDto>> createUser(
            @Valid @RequestBody UserRequestDto requestDto,
            HttpSession session) {

        permissionService.requireAdmin(session);

        ApiResponse<UserResponseDto> response = userService.createUser(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserById(
            @PathVariable Long id,
            HttpSession session) {

        permissionService.requireAdmin(session);

        ApiResponse<UserResponseDto> response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserByEmail(
            @PathVariable String email,
            HttpSession session) {

        permissionService.requireAdmin(session);

        ApiResponse<UserResponseDto> response = userService.getUserByEmail(email);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> getAllUsers(HttpSession session) {
        permissionService.requireAdmin(session);
        ApiResponse<List<UserResponseDto>> response = userService.getAllUsers();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequestDto requestDto,
            HttpSession session) {

        permissionService.requireAdmin(session);

        ApiResponse<UserResponseDto> response = userService.updateUser(id, requestDto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDto>> deleteUser(
            @PathVariable Long id,
            HttpSession session) {

        permissionService.requireAdmin(session);

        ApiResponse<UserResponseDto> response = userService.deleteUser(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> countUsers(HttpSession session) {
        permissionService.requireAdmin(session);
        ApiResponse<Long> response = userService.countUsers();
        return ResponseEntity.ok(response);
    }
}