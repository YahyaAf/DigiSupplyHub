package org.project.digital_logistics.mapper;

import org.project.digital_logistics.dto.ClientRequestDto;
import org.project.digital_logistics.dto.ClientResponseDto;
import org.project.digital_logistics.model.Client;
import org.project.digital_logistics.model.enums.Role;

public class ClientMapper {

    private ClientMapper() {
        throw new IllegalStateException("Utility class");
    }

    public static Client toEntity(ClientRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return Client.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .passwordHash(dto.getPassword())
                .role(Role.CLIENT)
                .active(dto.getActive() != null ? dto.getActive() : true)
                .phoneNumber(dto.getPhoneNumber())
                .address(dto.getAddress())
                .build();
    }

    public static ClientResponseDto toResponseDto(Client client) {
        if (client == null) {
            return null;
        }

        return ClientResponseDto.builder()
                .id(client.getId())
                .name(client.getName())
                .email(client.getEmail())
                .role(client.getRole())
                .active(client.getActive())
                .phoneNumber(client.getPhoneNumber())
                .address(client.getAddress())
                .build();
    }

    public static void updateEntityFromDto(ClientRequestDto dto, Client client) {
        if (dto == null || client == null) {
            return;
        }

        if (dto.getName() != null) {
            client.setName(dto.getName());
        }
        if (dto.getEmail() != null) {
            client.setEmail(dto.getEmail());
        }
        if (dto.getPassword() != null) {
            client.setPasswordHash(dto.getPassword());
        }
        if (dto.getActive() != null) {
            client.setActive(dto.getActive());
        }
        if (dto.getPhoneNumber() != null) {
            client.setPhoneNumber(dto.getPhoneNumber());
        }
        if (dto.getAddress() != null) {
            client.setAddress(dto.getAddress());
        }
    }
}