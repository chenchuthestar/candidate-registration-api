package com.example.candidateregistration.controller;

import com.example.candidateregistration.dto.LoginRequest;
import com.example.candidateregistration.dto.LoginResponse;
import com.example.candidateregistration.dto.SignupRequest;
import com.example.candidateregistration.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authenticationService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/signup")
    public ResponseEntity<LoginResponse> signup(@Valid @RequestBody SignupRequest request) {
        LoginResponse response = authenticationService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Test/development endpoint to create a user.
     * Remove this in production and use a proper user management system.
     * Sample: POST http://localhost:8080/api/auth/create-test-user
     * Body: { "email": "test@example.com", "password": "Test@123" }
     */
    @PostMapping("/create-test-user")
    public ResponseEntity<String> createTestUser(
            @RequestParam String email,
            @RequestParam String password) {
        authenticationService.createTestUser(email, password);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Test user created: " + email);
    }
}
