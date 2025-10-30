package org.project.digital_logistics.dto;

import org.project.digital_logistics.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ClientResponseDto {

    // Fields from User
    private Long id;
    private String name;
    private String email;
    private Role role;
    private Boolean active;

    // Client-specific fields
    private String phoneNumber;
    private String address;
}