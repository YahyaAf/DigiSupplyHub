package org.project.digital_logistics.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.project.digital_logistics.dto.ApiResponse;
import org.project. digital_logistics.dto.ClientRequestDto;
import org.project. digital_logistics.dto.ClientResponseDto;
import org.project. digital_logistics.service.ClientService;
import org.springframework.beans.factory.annotation. Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@CrossOrigin(origins = "*")
@Tag(name = "Clients", description = "Client Management")
public class ClientController {

    private final ClientService clientService;

    @Autowired
    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ClientResponseDto>> createClient(@Valid @RequestBody ClientRequestDto requestDto) {
        ApiResponse<ClientResponseDto> response = clientService.createClient(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClientResponseDto>> getClientById(@PathVariable Long id) {
        ApiResponse<ClientResponseDto> response = clientService. getClientById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<ClientResponseDto>> getClientByEmail(@PathVariable String email) {
        ApiResponse<ClientResponseDto> response = clientService.getClientByEmail(email);
        return ResponseEntity. ok(response);
    }

    @GetMapping("/phone/{phoneNumber}")
    public ResponseEntity<ApiResponse<ClientResponseDto>> getClientByPhoneNumber(@PathVariable String phoneNumber) {
        ApiResponse<ClientResponseDto> response = clientService.getClientByPhoneNumber(phoneNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ClientResponseDto>>> getAllClients() {
        ApiResponse<List<ClientResponseDto>> response = clientService.getAllClients();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ClientResponseDto>> updateClient(
            @PathVariable Long id,
            @Valid @RequestBody ClientRequestDto requestDto) {
        ApiResponse<ClientResponseDto> response = clientService.updateClient(id, requestDto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<ClientResponseDto>> deleteClient(@PathVariable Long id) {
        ApiResponse<ClientResponseDto> response = clientService.deleteClient(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> countClients() {
        ApiResponse<Long> response = clientService.countClients();
        return ResponseEntity.ok(response);
    }
}