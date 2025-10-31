package org.project.digital_logistics.service;

import org.project.digital_logistics.dto.ApiResponse;
import org.project.digital_logistics.dto.UserRequestDto;
import org.project.digital_logistics.dto.UserResponseDto;
import org.project.digital_logistics.exception.DuplicateResourceException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.mapper.UserMapper;
import org.project.digital_logistics.model.User;
import org.project.digital_logistics.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ApiResponse<UserResponseDto> createUser(UserRequestDto requestDto) {
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new DuplicateResourceException("User", "email", requestDto.getEmail());
        }

        String encodedPassword = passwordEncoder.encode(requestDto.getPassword());
        User user = UserMapper.toEntity(requestDto,encodedPassword);

        User savedUser = userRepository.save(user);
        UserResponseDto responseDto = UserMapper.toResponseDto(savedUser);

        return new ApiResponse<>("User created successfully", responseDto);
    }

    public ApiResponse<UserResponseDto> getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        UserResponseDto responseDto = UserMapper.toResponseDto(user);
        return new ApiResponse<>("User retrieved successfully", responseDto);
    }

    public ApiResponse<UserResponseDto> getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        UserResponseDto responseDto = UserMapper.toResponseDto(user);
        return new ApiResponse<>("User retrieved successfully", responseDto);
    }

    public ApiResponse<List<UserResponseDto>> getAllUsers() {
        List<UserResponseDto> users = userRepository.findAll()
                .stream()
                .map(UserMapper::toResponseDto)
                .collect(Collectors.toList());

        return new ApiResponse<>("Users retrieved successfully", users);
    }

    @Transactional
    public ApiResponse<UserResponseDto> updateUser(Long id, UserRequestDto requestDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (!user.getEmail().equals(requestDto.getEmail()) &&
                userRepository.existsByEmail(requestDto.getEmail())) {
            throw new DuplicateResourceException("User", "email", requestDto.getEmail());
        }

        String encodedPassword = passwordEncoder.encode(requestDto.getPassword());

        UserMapper.updateEntityFromDto(requestDto, user, encodedPassword);
        User updatedUser = userRepository.save(user);
        UserResponseDto responseDto = UserMapper.toResponseDto(updatedUser);

        return new ApiResponse<>("User updated successfully", responseDto);
    }

    @Transactional
    public ApiResponse<UserResponseDto> deleteUser(Long id) {
        Optional<User> userOpt = userRepository.findById(id);

        if (userOpt.isEmpty()) {
            throw new ResourceNotFoundException("User", "id", id);
        }

        User user = userOpt.get();
        UserResponseDto responseDto = UserMapper.toResponseDto(user);

        userRepository.deleteById(id);

        return new ApiResponse<>("User deleted successfully", responseDto);
    }

    public ApiResponse<Long> countUsers() {
        long count = userRepository.count();
        return new ApiResponse<>("Total users count", count);
    }
}