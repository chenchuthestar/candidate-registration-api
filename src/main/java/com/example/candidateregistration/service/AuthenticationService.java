package com.example.candidateregistration.service;

import com.example.candidateregistration.dto.LoginRequest;
import com.example.candidateregistration.dto.LoginResponse;
import com.example.candidateregistration.dto.SignupRequest;
import com.example.candidateregistration.exception.InvalidRequestException;
import com.example.candidateregistration.model.User;
import com.example.candidateregistration.repository.UserRepository;
import com.example.candidateregistration.security.JwtProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthenticationService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidRequestException("Invalid email or password"));

        if (!user.isActive()) {
            throw new InvalidRequestException("Your account is inactive. Please contact support.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidRequestException("Invalid email or password");
        }

        String token = jwtProvider.generateToken(user.getEmail());

        return new LoginResponse(token, user.getEmail(), "Login successful");
    }

    public LoginResponse signup(SignupRequest request) {
        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new InvalidRequestException("Email is already registered");
        }

        // Validate password confirmation
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new InvalidRequestException("Passwords do not match");
        }

        // Validate password length
        if (request.getPassword().length() < 6) {
            throw new InvalidRequestException("Password must be at least 6 characters");
        }

        // Validate role is not empty
        if (request.getRole() == null || request.getRole().trim().isEmpty()) {
            throw new InvalidRequestException("Role is required");
        }

        // Create new user
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole().trim())
                .createdAt(System.currentTimeMillis())
                .active(true)
                .build();

        userRepository.save(user);

        // Generate token and return login response
        String token = jwtProvider.generateToken(user.getEmail());

        return new LoginResponse(token, user.getEmail(), "Signup successful! Welcome aboard.");
    }

    /**
     * Create a test user if needed (optional endpoint for development).
     * In production, admin would create users via a separate user management system.
     */
    public void createTestUser(String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new InvalidRequestException("Email already registered");
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .role("Recruiter")
                .active(true)
                .build();

        userRepository.save(user);
    }
}
