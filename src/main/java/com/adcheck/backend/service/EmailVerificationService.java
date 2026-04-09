package com.adcheck.backend.service;

import com.adcheck.backend.entity.EmailVerificationCode;
import com.adcheck.backend.repository.EmailVerificationCodeRepository;
import com.adcheck.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationCodeRepository verificationCodeRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final Random random = new SecureRandom();

    @Value("${spring.mail.username:}")
    private String fromEmail;

    private static final int CODE_EXPIRATION_MINUTES = 10;

    @Transactional
    public String sendVerificationCode(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        String code = generateCode();
        EmailVerificationCode verification = EmailVerificationCode.builder()
                .email(email)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(CODE_EXPIRATION_MINUTES))
                .build();

        verificationCodeRepository.save(verification);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        if (fromEmail != null && !fromEmail.isBlank()) {
            message.setFrom(fromEmail);
        }
        message.setSubject("[ADCheck] 이메일 인증 코드");
        message.setText(String.format("인증 코드: %s%n유효 시간: %d분%n잘못 신청하셨다면 이 메일을 무시하세요.",
                code, CODE_EXPIRATION_MINUTES));
        mailSender.send(message);

        log.info("[EmailVerification] code sent to {}", email);
        return code;
    }

    @Transactional
    public void verifyCode(String email, String code) {
        EmailVerificationCode latest = verificationCodeRepository
                .findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new IllegalArgumentException("인증 요청 기록을 찾을 수 없습니다."));

        if (latest.isExpired()) {
            throw new IllegalArgumentException("인증 코드가 만료되었습니다. 다시 요청해주세요.");
        }

        if (!latest.getCode().equals(code)) {
            throw new IllegalArgumentException("인증 코드가 일치하지 않습니다.");
        }

        latest.markVerified();
    }

    public boolean isVerified(String email) {
        return verificationCodeRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .filter(code -> !code.isExpired())
                .map(EmailVerificationCode::isVerified)
                .orElse(false);
    }

    private String generateCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}
