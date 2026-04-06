package com.adcheck.backend.config;

import com.adcheck.backend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && jwtUtil.validateToken(token)) {
            try {
                Long userId = jwtUtil.getUserId(token);

                userRepository.findById(userId).ifPresent(user -> {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    user, null, Collections.emptyList()
                            );
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            } catch (Exception e) {
                log.warn("JWT 인증 처리 실패: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        // 1) 표준 Authorization 헤더 우선
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer)) {
            if (bearer.startsWith("Bearer ")) {
                return bearer.substring(7);
            }
            // 프런트가 접두어 없이 보낸 경우도 허용
            return bearer;
        }

        // 2) 쿠키(token / Authorization)에서도 찾아서 허용
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("token".equalsIgnoreCase(cookie.getName())
                        || "Authorization".equalsIgnoreCase(cookie.getName())) {
                    String value = cookie.getValue();
                    if (StringUtils.hasText(value)) {
                        return value.startsWith("Bearer ") ? value.substring(7) : value;
                    }
                }
            }
        }

        // 3) 쿼리스트링 ?token=... 도 최후 수단으로 허용 (모바일/테스트 편의)
        String paramToken = request.getParameter("token");
        if (StringUtils.hasText(paramToken)) {
            return paramToken.startsWith("Bearer ") ? paramToken.substring(7) : paramToken;
        }

        return null;
    }
}
