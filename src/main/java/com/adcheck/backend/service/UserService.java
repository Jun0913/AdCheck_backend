package com.adcheck.backend.service;

import com.adcheck.backend.config.JwtUtil;
import com.adcheck.backend.dto.LoginRequestDto;
import com.adcheck.backend.dto.LoginResponseDto;
import com.adcheck.backend.dto.RegisterRequestDto;
import com.adcheck.backend.entity.User;
import com.adcheck.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailVerificationService emailVerificationService;

    /**
     * Register a new local user.
     */
    @Transactional
    public void register(RegisterRequestDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        if (!emailVerificationService.isVerified(dto.getEmail())) {
            throw new IllegalArgumentException("이메일 인증이 완료되지 않아 회원가입을 진행할 수 없습니다.");
        }

        User user = User.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .nickname(dto.getNickname())
                .provider(User.Provider.LOCAL)
                .build();

        userRepository.save(user);
        log.info("회원가입 완료: {}", dto.getEmail());
    }

    /**
     * Login with email/password and return JWT token + nickname.
     */
    public LoginResponseDto login(LoginRequestDto dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        log.info("로그인 성공: {}", dto.getEmail());
        return new LoginResponseDto(token, user.getNickname());
    }

    /**
     * Change password for authenticated local user.
     */
    @Transactional
    public void changePassword(User user, String currentPassword, String newPassword) {
        if (user.getProvider() != User.Provider.LOCAL) {
            throw new IllegalArgumentException("소셜 로그인 계정은 비밀번호를 변경할 수 없습니다.");
        }

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("이전과 동일한 비밀번호는 사용할 수 없습니다.");
        }

        user.changePassword(passwordEncoder.encode(newPassword));
        log.info("비밀번호 변경 완료: {}", user.getEmail());
    }
}
