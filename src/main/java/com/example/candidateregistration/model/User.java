package com.example.candidateregistration.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 255)
    private String password; // Store as bcrypt hash (Spring Security will handle)

    @Column(nullable = false, length = 50)
    private String role; // e.g., "HR", "Recruiter", "Manager", "Admin"

    @Column(nullable = false)
    private String active = "pending";

    @Column(nullable = false, updatable = false)
    private Long createdAt = System.currentTimeMillis();
}
