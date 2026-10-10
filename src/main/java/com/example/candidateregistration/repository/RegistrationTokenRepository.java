package com.example.candidateregistration.repository;
import com.example.candidateregistration.model.RegistrationToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;
public interface RegistrationTokenRepository extends JpaRepository<RegistrationToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RegistrationToken> findByTokenHash(String tokenHash);
}
