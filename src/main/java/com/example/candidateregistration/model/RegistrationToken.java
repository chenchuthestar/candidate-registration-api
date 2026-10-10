package com.example.candidateregistration.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
@Entity @Table(name = "registration_tokens") @Getter @Setter
public class RegistrationToken {
    public enum Purpose { VERIFY_EMAIL, REVIEW_ADMIN }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 64) private String tokenHash;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Purpose purpose;
    @Column(nullable = false) private Instant expiresAt;
    private Instant consumedAt;
    private String reviewerEmail;
}
