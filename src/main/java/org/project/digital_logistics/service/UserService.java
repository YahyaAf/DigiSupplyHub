package org.project.digital_logistics.service;

import org.project.digital_logistics.dto.UserRequestDto;
import org.project.digital_logistics.dto.UserResponseDto;
import org.project.digital_logistics.exception.DuplicateResourceException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.mapper.UserMapper;
import org.project.digital_logistics.model.User;
import org.project.digital_logistics.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponseDto createUser(UserRequestDto requestDto) {
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new DuplicateResourceException("User", "email", requestDto.getEmail());
        }

        User user = UserMapper.toEntity(requestDto);
        User savedUser = userRepository.save(user);
        return UserMapper.toResponseDto(savedUser);
    }

    public UserResponseDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        return UserMapper.toResponseDto(user);
    }

    public UserResponseDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        return UserMapper.toResponseDto(user);
    }

    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponseDto updateUser(Long id, UserRequestDto requestDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (!user.getEmail().equals(requestDto.getEmail()) &&
                userRepository.existsByEmail(requestDto.getEmail())) {
            throw new DuplicateResourceException("User", "email", requestDto.getEmail());
        }

        UserMapper.updateEntityFromDto(requestDto, user);
        User updatedUser = userRepository.save(user);

        return UserMapper.toResponseDto(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", "id", id);
        }

        userRepository.deleteById(id);
    }

    public long countUsers() {
        return userRepository.count();
    }
}