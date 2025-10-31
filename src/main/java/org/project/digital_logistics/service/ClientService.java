package org.project.digital_logistics.service;

import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.ClientRequestDto;
import org.project.digital_logistics.dto.ClientResponseDto;
import org.project.digital_logistics.exception.DuplicateResourceException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.mapper.ClientMapper;
import org.project.digital_logistics.model.Client;
import org.project.digital_logistics.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ClientService {

    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public ClientService(ClientRepository clientRepository, PasswordEncoder passwordEncoder) {
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ApiResponse<ClientResponseDto> createClient(ClientRequestDto requestDto) {
        if (clientRepository.existsByEmail(requestDto.getEmail())) {
            throw new DuplicateResourceException("Client", "email", requestDto.getEmail());
        }

        if (clientRepository.existsByPhoneNumber(requestDto.getPhoneNumber())) {
            throw new DuplicateResourceException("Client", "phoneNumber", requestDto.getPhoneNumber());
        }

        String encodedPassword = passwordEncoder.encode(requestDto.getPassword());

        Client client = ClientMapper.toEntity(requestDto, encodedPassword);
        Client savedClient = clientRepository.save(client);
        ClientResponseDto responseDto = ClientMapper.toResponseDto(savedClient);

        return new ApiResponse<>("Client created successfully", responseDto);
    }

    public ApiResponse<ClientResponseDto> getClientById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", id));

        ClientResponseDto responseDto = ClientMapper.toResponseDto(client);
        return new ApiResponse<>("Client retrieved successfully", responseDto);
    }

    public ApiResponse<ClientResponseDto> getClientByEmail(String email) {
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "email", email));

        ClientResponseDto responseDto = ClientMapper.toResponseDto(client);
        return new ApiResponse<>("Client retrieved successfully", responseDto);
    }

    public ApiResponse<ClientResponseDto> getClientByPhoneNumber(String phoneNumber) {
        Client client = clientRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "phoneNumber", phoneNumber));

        ClientResponseDto responseDto = ClientMapper.toResponseDto(client);
        return new ApiResponse<>("Client retrieved successfully", responseDto);
    }

    public ApiResponse<List<ClientResponseDto>> getAllClients() {
        List<ClientResponseDto> clients = clientRepository.findAll()
                .stream()
                .map(ClientMapper::toResponseDto)
                .collect(Collectors.toList());

        return new ApiResponse<>("Clients retrieved successfully", clients);
    }

    @Transactional
    public ApiResponse<ClientResponseDto> updateClient(Long id, ClientRequestDto requestDto) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", id));

        if (!client.getEmail().equals(requestDto.getEmail()) &&
                clientRepository.existsByEmail(requestDto.getEmail())) {
            throw new DuplicateResourceException("Client", "email", requestDto.getEmail());
        }

        if (!client.getPhoneNumber().equals(requestDto.getPhoneNumber()) &&
                clientRepository.existsByPhoneNumber(requestDto.getPhoneNumber())) {
            throw new DuplicateResourceException("Client", "phoneNumber", requestDto.getPhoneNumber());
        }

        String passwordEnder = passwordEncoder.encode(requestDto.getPassword());

        ClientMapper.updateEntityFromDto(requestDto, client, passwordEnder);
        Client updatedClient = clientRepository.save(client);
        ClientResponseDto responseDto = ClientMapper.toResponseDto(updatedClient);

        return new ApiResponse<>("Client updated successfully", responseDto);
    }

    @Transactional
    public ApiResponse<ClientResponseDto> deleteClient(Long id) {
        Optional<Client> clientOpt = clientRepository.findById(id);

        if (clientOpt.isEmpty()) {
            throw new ResourceNotFoundException("Client", "id", id);
        }

        Client client = clientOpt.get();
        ClientResponseDto responseDto = ClientMapper.toResponseDto(client);

        clientRepository.deleteById(id);

        return new ApiResponse<>("Client deleted successfully", responseDto);
    }

    public ApiResponse<Long> countClients() {
        long count = clientRepository.count();
        return new ApiResponse<>("Total clients count", count);
    }
}