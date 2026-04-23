package com.adcheck.backend.integration;

import com.adcheck.backend.config.JwtUtil;
import com.adcheck.backend.entity.AnalysisResult;
import com.adcheck.backend.entity.User;
import com.adcheck.backend.repository.AnalysisResultRepository;
import com.adcheck.backend.repository.UserRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AnalysisResultRepository analysisResultRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        analysisResultRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("/login returns token cookie and body on success")
    void login_success_returnsJwtAndCookie() throws Exception {
        userRepository.save(User.builder()
                .email("user@example.com")
                .password(passwordEncoder.encode("secret123"))
                .nickname("닉")
                .provider(User.Provider.LOCAL)
                .build());

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.nickname").value("닉"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("token=")));
    }

    @Test
    @DisplayName("/history requires authentication")
    void history_requires_authentication() throws Exception {
        mockMvc.perform(get("/history"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("/history returns only the current user's items when token is valid")
    void history_returns_items_for_authenticated_user() throws Exception {
        User user = userRepository.save(User.builder()
                .email("member@example.com")
                .password(passwordEncoder.encode("pass1234"))
                .nickname("멤버")
                .provider(User.Provider.LOCAL)
                .build());

        // 다른 유저 데이터가 섞여 있어도 필터링 되는지 확인
        User other = userRepository.save(User.builder()
                .email("other@example.com")
                .password(passwordEncoder.encode("pass1234"))
                .nickname("다른유저")
                .provider(User.Provider.LOCAL)
                .build());

        analysisResultRepository.save(AnalysisResult.builder()
                .user(other)
                .inputType(AnalysisResult.InputType.TEXT)
                .inputContent("other")
                .originalText("other text")
                .overallSuspicionLevel("SAFE")
                .overallScore(0.1)
                .summary("other summary")
                .sentenceResultsJson("[]")
                .build());

        AnalysisResult mine = analysisResultRepository.save(AnalysisResult.builder()
                .user(user)
                .inputType(AnalysisResult.InputType.TEXT)
                .inputContent("hello")
                .originalText("hello text")
                .overallSuspicionLevel("RISK")
                .overallScore(0.9)
                .summary("summary")
                .sentenceResultsJson("[]")
                .build());

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        mockMvc.perform(get("/history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(mine.getId()))
                .andExpect(jsonPath("$.content[0].inputType").value("TEXT"))
                .andExpect(jsonPath("$.content[0].inputContent").value("hello"));
    }

    @Test
    @DisplayName("DELETE /history hides current user's items without deleting rows")
    void history_delete_hides_current_user_items() throws Exception {
        User user = userRepository.save(User.builder()
                .email("delete-me@example.com")
                .password(passwordEncoder.encode("pass1234"))
                .nickname("member")
                .provider(User.Provider.LOCAL)
                .build());

        AnalysisResult mine = analysisResultRepository.save(AnalysisResult.builder()
                .user(user)
                .inputType(AnalysisResult.InputType.TEXT)
                .inputContent("hidden")
                .originalText("hidden text")
                .overallSuspicionLevel("정상")
                .overallScore(0.0)
                .summary("summary")
                .sentenceResultsJson("[]")
                .build());

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        mockMvc.perform(delete("/history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hiddenCount").value(1));

        mockMvc.perform(get("/history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        mockMvc.perform(get("/history/" + mine.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());

        org.assertj.core.api.Assertions.assertThat(
                analysisResultRepository.findById(mine.getId())
        ).isPresent();
    }

    @Test
    @DisplayName("DELETE /history/{id} hides only the requested current user's item")
    void history_delete_by_id_hides_single_current_user_item() throws Exception {
        User user = userRepository.save(User.builder()
                .email("delete-one@example.com")
                .password(passwordEncoder.encode("pass1234"))
                .nickname("member")
                .provider(User.Provider.LOCAL)
                .build());

        AnalysisResult first = analysisResultRepository.save(AnalysisResult.builder()
                .user(user)
                .inputType(AnalysisResult.InputType.TEXT)
                .inputContent("first")
                .originalText("first text")
                .overallSuspicionLevel("정상")
                .overallScore(0.0)
                .summary("summary")
                .sentenceResultsJson("[]")
                .build());

        AnalysisResult second = analysisResultRepository.save(AnalysisResult.builder()
                .user(user)
                .inputType(AnalysisResult.InputType.TEXT)
                .inputContent("second")
                .originalText("second text")
                .overallSuspicionLevel("정상")
                .overallScore(0.0)
                .summary("summary")
                .sentenceResultsJson("[]")
                .build());

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        mockMvc.perform(delete("/history/" + first.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hiddenCount").value(1));

        mockMvc.perform(get("/history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(second.getId()));

        mockMvc.perform(get("/history/" + first.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}

