package org.project.digital_logistics.service;

import jakarta.servlet.http.HttpSession;
import org.project.digital_logistics.dto.auth.AuthResponseDto;
import org.project.digital_logistics.dto.auth.LoginRequestDto;
import org.project.digital_logistics.dto.auth.RegisterRequestDto;
import org.project.digital_logistics.exception.DuplicateResourceException;
import org.project.digital_logistics.exception.ResourceNotFoundException;
import org.project.digital_logistics.mapper.AuthMapper;
import org.project.digital_logistics.model.Client;
import org.project.digital_logistics.model.User;
import org.project.digital_logistics.repository.ClientRepository;
import org.project.digital_logistics.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String SESSION_USER_KEY = "authenticated_user";

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthService(ClientRepository clientRepository,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponseDto register(RegisterRequestDto registerDto, HttpSession session) {
        if (clientRepository.existsByEmail(registerDto.getEmail())) {
            throw new DuplicateResourceException("Client", "email", registerDto.getEmail());
        }

        if (clientRepository.existsByPhoneNumber(registerDto.getPhoneNumber())) {
            throw new DuplicateResourceException("Client", "phoneNumber", registerDto.getPhoneNumber());
        }

        String encodedPassword = passwordEncoder.encode(registerDto.getPassword());

        Client client = AuthMapper.toClientEntity(registerDto, encodedPassword);

        Client savedClient = clientRepository.save(client);

        session.setAttribute(SESSION_USER_KEY, savedClient.getId());

        return AuthMapper.toAuthResponseDto(savedClient, "Registration successful");
    }

    @Transactional(readOnly = true)
    public AuthResponseDto login(LoginRequestDto loginDto, HttpSession session) {
        User user = userRepository.findByEmail(loginDto.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", loginDto.getEmail()));

        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        if (!user.getActive()) {
            throw new IllegalStateException("Account is inactive. Please contact support.");
        }

        session.setAttribute(SESSION_USER_KEY, user.getId());

        return AuthMapper.toAuthResponseDto(user, "Login successful");
    }

    public String logout(HttpSession session) {
        session.removeAttribute(SESSION_USER_KEY);
        session.invalidate();
        return "Logout successful";
    }

    @Transactional(readOnly = true)
    public AuthResponseDto getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_KEY);

        if (userId == null) {
            throw new IllegalStateException("Not authenticated. Please login.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        return AuthMapper.toAuthResponseDto(user, "User retrieved successfully");
    }

    public boolean isAuthenticated(HttpSession session) {
        return session.getAttribute(SESSION_USER_KEY) != null;
    }
}