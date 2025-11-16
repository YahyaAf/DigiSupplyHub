package org.project.digital_logistics.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.ClientRequestDto;
import org.project.digital_logistics.dto.ClientResponseDto;
import org.project.digital_logistics.exception.DuplicateResourceException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.model.Client;
import org.project.digital_logistics.repository.ClientRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ClientService clientService;

    private Client client;
    private ClientRequestDto requestDto;

    @BeforeEach
    void setUp() {
        // Setup ClientRequestDto
        requestDto = ClientRequestDto.builder()
                .name("John Doe")
                .email("john.doe@example.com")
                .phoneNumber("0612345678")
                .address("123 Main Street, Casablanca")
                .password("SecurePassword123")
                .build();

        // Setup Client Entity
        client = Client.builder()
                .id(1L)
                .name("John Doe")
                .email("john.doe@example.com")
                .phoneNumber("0612345678")
                .address("123 Main Street, Casablanca")
                .passwordHash("encodedPassword123")
                .active(true)
                .build();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // CREATE CLIENT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void createClient_Success() {
        // Given
        when(clientRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(clientRepository.existsByPhoneNumber("0612345678")).thenReturn(false);
        when(passwordEncoder.encode("SecurePassword123")).thenReturn("encodedPassword123");
        when(clientRepository.save(any(Client.class))).thenReturn(client);

        // When
        ApiResponse<ClientResponseDto> response = clientService.createClient(requestDto);

        // Then
        assertNotNull(response);
        assertEquals("Client created successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals("john.doe@example.com", response.getData().getEmail());
        assertEquals("0612345678", response.getData().getPhoneNumber());

        verify(clientRepository).existsByEmail("john.doe@example.com");
        verify(clientRepository).existsByPhoneNumber("0612345678");
        verify(passwordEncoder).encode("SecurePassword123");
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    void createClient_DuplicateEmail_ThrowsException() {
        // Given
        when(clientRepository.existsByEmail("john.doe@example.com")).thenReturn(true);

        // When & Then
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> clientService.createClient(requestDto)
        );

        assertTrue(exception.getMessage().contains("email"));
        assertTrue(exception.getMessage().contains("john.doe@example.com"));

        verify(clientRepository).existsByEmail("john.doe@example.com");
        verify(clientRepository, never()).existsByPhoneNumber(anyString());
        verify(clientRepository, never()).save(any());
    }

    @Test
    void createClient_DuplicatePhoneNumber_ThrowsException() {
        // Given
        when(clientRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(clientRepository.existsByPhoneNumber("0612345678")).thenReturn(true);

        // When & Then
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> clientService.createClient(requestDto)
        );

        assertTrue(exception.getMessage().contains("phoneNumber"));
        assertTrue(exception.getMessage().contains("0612345678"));

        verify(clientRepository).existsByEmail("john.doe@example.com");
        verify(clientRepository).existsByPhoneNumber("0612345678");
        verify(clientRepository, never()).save(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // GET CLIENT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void getClientById_Success() {
        // Given
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        // When
        ApiResponse<ClientResponseDto> response = clientService.getClientById(1L);

        // Then
        assertNotNull(response);
        assertEquals("Client retrieved successfully", response.getMessage());
        assertEquals(1L, response.getData().getId());
        assertEquals("john.doe@example.com", response.getData().getEmail());

        verify(clientRepository).findById(1L);
    }

    @Test
    void getClientById_NotFound_ThrowsException() {
        // Given
        when(clientRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> clientService.getClientById(999L)
        );

        assertTrue(exception.getMessage().contains("Client"));
        assertTrue(exception.getMessage().contains("id"));

        verify(clientRepository).findById(999L);
    }

    @Test
    void getClientByEmail_Success() {
        // Given
        when(clientRepository.findByEmail("john.doe@example.com"))
                .thenReturn(Optional.of(client));

        // When
        ApiResponse<ClientResponseDto> response =
                clientService.getClientByEmail("john.doe@example.com");

        // Then
        assertNotNull(response);
        assertEquals("Client retrieved successfully", response.getMessage());
        assertEquals("john.doe@example.com", response.getData().getEmail());

        verify(clientRepository).findByEmail("john.doe@example.com");
    }

    @Test
    void getClientByEmail_NotFound_ThrowsException() {
        // Given
        when(clientRepository.findByEmail("notfound@example.com"))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> clientService.getClientByEmail("notfound@example.com")
        );

        assertTrue(exception.getMessage().contains("Client"));
        assertTrue(exception.getMessage().contains("email"));

        verify(clientRepository).findByEmail("notfound@example.com");
    }

    @Test
    void getClientByPhoneNumber_Success() {
        // Given
        when(clientRepository.findByPhoneNumber("0612345678"))
                .thenReturn(Optional.of(client));

        // When
        ApiResponse<ClientResponseDto> response =
                clientService.getClientByPhoneNumber("0612345678");

        // Then
        assertNotNull(response);
        assertEquals("Client retrieved successfully", response.getMessage());
        assertEquals("0612345678", response.getData().getPhoneNumber());

        verify(clientRepository).findByPhoneNumber("0612345678");
    }

    @Test
    void getClientByPhoneNumber_NotFound_ThrowsException() {
        // Given
        when(clientRepository.findByPhoneNumber("0600000000"))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> clientService.getClientByPhoneNumber("0600000000")
        );

        assertTrue(exception.getMessage().contains("Client"));
        assertTrue(exception.getMessage().contains("phoneNumber"));

        verify(clientRepository).findByPhoneNumber("0600000000");
    }

    @Test
    void getAllClients_Success() {
        // Given
        Client client2 = Client.builder()
                .id(2L)
                .name("Jane Smith")
                .email("jane.smith@example.com")
                .phoneNumber("0698765432")
                .address("456 Second Ave")
                .passwordHash("encodedPassword456")
                .active(true)
                .build();

        List<Client> clients = Arrays.asList(client, client2);
        when(clientRepository.findAll()).thenReturn(clients);

        // When
        ApiResponse<List<ClientResponseDto>> response = clientService.getAllClients();

        // Then
        assertNotNull(response);
        assertEquals("Clients retrieved successfully", response.getMessage());
        assertEquals(2, response.getData().size());
        assertEquals("john.doe@example.com", response.getData().get(0).getEmail());
        assertEquals("jane.smith@example.com", response.getData().get(1).getEmail());

        verify(clientRepository).findAll();
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // UPDATE CLIENT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void updateClient_Success() {
        // Given
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(passwordEncoder.encode("SecurePassword123")).thenReturn("newEncodedPassword");
        when(clientRepository.save(any(Client.class))).thenReturn(client);

        // When
        ApiResponse<ClientResponseDto> response = clientService.updateClient(1L, requestDto);

        // Then
        assertNotNull(response);
        assertEquals("Client updated successfully", response.getMessage());

        verify(clientRepository).findById(1L);
        verify(passwordEncoder).encode("SecurePassword123");
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    void updateClient_NotFound_ThrowsException() {
        // Given
        when(clientRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> clientService.updateClient(999L, requestDto)
        );

        assertTrue(exception.getMessage().contains("Client"));

        verify(clientRepository).findById(999L);
        verify(clientRepository, never()).save(any());
    }

    @Test
    void updateClient_DuplicateEmail_ThrowsException() {
        // Given
        requestDto.setEmail("newemail@example.com");
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(clientRepository.existsByEmail("newemail@example.com")).thenReturn(true);

        // When & Then
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> clientService.updateClient(1L, requestDto)
        );

        assertTrue(exception.getMessage().contains("email"));

        verify(clientRepository).findById(1L);
        verify(clientRepository).existsByEmail("newemail@example.com");
        verify(clientRepository, never()).save(any());
    }

    @Test
    void updateClient_DuplicatePhoneNumber_ThrowsException() {
        // Given
        requestDto.setPhoneNumber("0698765432");
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(clientRepository.existsByPhoneNumber("0698765432")).thenReturn(true);

        // When & Then
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> clientService.updateClient(1L, requestDto)
        );

        assertTrue(exception.getMessage().contains("phoneNumber"));

        verify(clientRepository).findById(1L);
        verify(clientRepository).existsByPhoneNumber("0698765432");
        verify(clientRepository, never()).save(any());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // DELETE CLIENT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void deleteClient_Success() {
        // Given
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        doNothing().when(clientRepository).deleteById(1L);

        // When
        ApiResponse<ClientResponseDto> response = clientService.deleteClient(1L);

        // Then
        assertNotNull(response);
        assertEquals("Client deleted successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(1L, response.getData().getId());

        verify(clientRepository).findById(1L);
        verify(clientRepository).deleteById(1L);
    }

    @Test
    void deleteClient_NotFound_ThrowsException() {
        // Given
        when(clientRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> clientService.deleteClient(999L)
        );

        assertTrue(exception.getMessage().contains("Client"));

        verify(clientRepository).findById(999L);
        verify(clientRepository, never()).deleteById(anyLong());
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // COUNT TESTS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    @Test
    void countClients_Success() {
        // Given
        when(clientRepository.count()).thenReturn(25L);

        // When
        ApiResponse<Long> response = clientService.countClients();

        // Then
        assertNotNull(response);
        assertEquals("Total clients count", response.getMessage());
        assertEquals(25L, response.getData());

        verify(clientRepository).count();
    }
}