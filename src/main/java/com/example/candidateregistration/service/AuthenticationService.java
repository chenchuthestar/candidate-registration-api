package com.example.candidateregistration.service;

import com.example.candidateregistration.dto.LoginRequest;
import com.example.candidateregistration.dto.LoginResponse;
import com.example.candidateregistration.dto.SignupRequest;
import com.example.candidateregistration.exception.InvalidRequestException;
import com.example.candidateregistration.model.User;
import com.example.candidateregistration.repository.UserRepository;
import com.example.candidateregistration.security.JwtProvider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtProvider jwtProvider;

	@Autowired
	private EmailService emailService;

	public AuthenticationService(UserRepository userRepository, PasswordEncoder passwordEncoder,
			JwtProvider jwtProvider) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtProvider = jwtProvider;
	}

	public LoginResponse login(LoginRequest request) {
		User user = userRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new InvalidRequestException("Invalid email or password"));

		if (!user.getEmail().equalsIgnoreCase("approved")) {
			throw new InvalidRequestException("Your account is pending. Please contact support.");
		}

		if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			throw new InvalidRequestException("Invalid email or password");
		}

		String token = jwtProvider.generateToken(user.getEmail());

		return new LoginResponse(token, user.getEmail(), "Login successful");
	}

    public LoginResponse signup(SignupRequest request) {
        return null;
    }

	/**
	 * Create a test user if needed (optional endpoint for development). In
	 * production, admin would create users via a separate user management system.
	 */
	public void createTestUser(String email, String password) {
		if (userRepository.findByEmail(email).isPresent()) {
			throw new InvalidRequestException("Email already registered");
		}

		User user = User.builder().email(email).password(passwordEncoder.encode(password)).role("Recruiter")
				.active("pending").build();

		userRepository.save(user);
	}

	public void approve(Long id, String email) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("User not found"));

		if (!user.getEmail().equals(email)) {
			throw new RuntimeException("Email does not match user ID");
		}

		user.setActive("pending");
		userRepository.save(user);
	}
	public void reject(Long id, String email) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("User not found"));

		if (!user.getEmail().equals(email)) {
			throw new RuntimeException("Email does not match user ID");
		}

		user.setActive("pending");
		userRepository.save(user);
	}
}
