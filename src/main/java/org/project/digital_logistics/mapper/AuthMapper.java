package org.project.digital_logistics.mapper;

import org.project.digital_logistics.dto.auth.AuthResponseDto;
import org.project.digital_logistics.dto.auth.RegisterRequestDto;
import org.project.digital_logistics.model.Client;
import org.project.digital_logistics.model.User;
import org.project.digital_logistics.model.enums.Role;

public class AuthMapper {

    private AuthMapper() {
        throw new IllegalStateException("Utility class");
    }

    public static Client toClientEntity(RegisterRequestDto dto, String encodedPassword) {
        if (dto == null) {
            return null;
        }

        return Client.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .passwordHash(encodedPassword)
                .role(Role.CLIENT)
                .active(true)
                .phoneNumber(dto.getPhoneNumber())
                .address(dto.getAddress())
                .build();
    }

    public static AuthResponseDto toAuthResponseDto(User user, String message) {
        if (user == null) {
            return null;
        }

        AuthResponseDto.AuthResponseDtoBuilder responseBuilder = AuthResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .message(message);

        if (user instanceof Client) {
            Client client = (Client) user;
            responseBuilder
                    .phoneNumber(client.getPhoneNumber())
                    .address(client.getAddress());
        }

        return responseBuilder.build();
    }
}