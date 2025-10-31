package org.project.digital_logistics.dto.auth;

import org.project.digital_logistics.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDto {

    private Long id;
    private String name;
    private String email;
    private Role role;
    private String message;

    private String phoneNumber;
    private String address;
}