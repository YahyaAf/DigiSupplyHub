package org.project.digital_logistics.mapper;

import org.project.digital_logistics.dto.UserRequestDto;
import org.project.digital_logistics.dto.UserResponseDto;
import org.project.digital_logistics.model.User;

public class UserMapper {

    private UserMapper() {
        throw new IllegalStateException("Utility class");
    }

    public static User toEntity(UserRequestDto dto,  String encodedPassword) {
        if (dto == null) {
            return null;
        }

        return User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .passwordHash(encodedPassword)
                .role(dto.getRole())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();
    }

    public static UserResponseDto toResponseDto(User user) {
        if (user == null) {
            return null;
        }

        return UserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .active(user.getActive())
                .build();
    }

    public static void updateEntityFromDto(UserRequestDto dto, User user, String encodedPassword) {
        if (dto == null || user == null) {
            return;
        }

        if (dto.getName() != null) {
            user.setName(dto.getName());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getPassword() != null) {
            user.setPasswordHash(encodedPassword);
        }
        if (dto.getRole() != null) {
            user.setRole(dto.getRole());
        }
        if (dto.getActive() != null) {
            user.setActive(dto.getActive());
        }
    }
}