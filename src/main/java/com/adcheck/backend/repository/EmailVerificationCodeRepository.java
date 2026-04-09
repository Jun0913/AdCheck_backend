package com.adcheck.backend.repository;

import com.adcheck.backend.entity.EmailVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, Long> {

    Optional<EmailVerificationCode> findTopByEmailOrderByCreatedAtDesc(String email);
}
