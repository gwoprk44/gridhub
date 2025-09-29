package com.gridhub.gridhub.domain.prediction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridhub.gridhub.domain.f1data.entity.Driver;
import com.gridhub.gridhub.domain.f1data.entity.Race;
import com.gridhub.gridhub.domain.f1data.repository.DriverRepository;
import com.gridhub.gridhub.domain.f1data.repository.RaceRepository;
import com.gridhub.gridhub.domain.prediction.dto.PredictionRequest;
import com.gridhub.gridhub.domain.prediction.repository.PredictionRepository;
import com.gridhub.gridhub.domain.user.entity.User;
import com.gridhub.gridhub.domain.user.entity.UserRole;
import com.gridhub.gridhub.domain.user.repository.UserRepository;
import com.gridhub.gridhub.global.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PredictionControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RaceRepository raceRepository;
    @Autowired
    private DriverRepository driverRepository;
    @Autowired
    private PredictionRepository predictionRepository;
    @Autowired
    private JwtUtil jwtUtil;

    private String userToken;
    private Race predictableRace;

    @BeforeEach
    void setUp() {
        predictionRepository.deleteAllInBatch();
        driverRepository.deleteAllInBatch();
        raceRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        // 사용자 생성
        User user = userRepository.save(User.builder().email("user@test.com").password("encoded").nickname("user").role(UserRole.USER).build());
        User user1 = userRepository.save(User.builder().email("user1@test.com").password("pwd").nickname("TopRanker").role(UserRole.USER).build());
        User user2 = userRepository.save(User.builder().email("user2@test.com").password("pwd").nickname("MidRanker").role(UserRole.USER).build());
        User user3 = userRepository.save(User.builder().email("user3@test.com").password("pwd").nickname("LowRanker").role(UserRole.USER).build());

        // 포인트 설정
        user1.addPoints(100);
        user2.addPoints(50);
        user3.addPoints(10);
        userRepository.saveAll(List.of(user1, user2, user3));

        userToken = jwtUtil.createToken(user.getEmail(), user.getRole());

        // 레이스 생성
        predictableRace = raceRepository.save(Race.builder()
                .id(1L)
                .year(ZonedDateTime.now().getYear())
                .dateStart(ZonedDateTime.now().plusDays(1))
                .dateEnd(ZonedDateTime.now().plusDays(1).plusHours(2))
                .meetingKey(1L)
                .meetingName("Test GP")
                .sessionName("Race")
                .countryName("Testland")
                .circuitShortName("TST")
                .build());

        // 드라이버 생성
        driverRepository.save(Driver.builder().id(1).build());
        driverRepository.save(Driver.builder().id(2).build());
        driverRepository.save(Driver.builder().id(3).build());
    }

    @DisplayName("POST /api/predictions/races/{raceId} - 예측 제출 성공")
    @Test
    void createPrediction_Success() throws Exception {
        // given
        PredictionRequest request = new PredictionRequest(1, 2, 3);

        // when & then
        mockMvc.perform(post("/api/predictions/races/" + predictableRace.getId())
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andDo(print());
    }

    @DisplayName("POST /api/predictions/races/{raceId} - 예측 제출 실패 (드라이버 중복)")
    @Test
    void createPrediction_Fail_DuplicateDriver() throws Exception {
        // given
        PredictionRequest request = new PredictionRequest(1, 1, 3); // 1번 중복

        // when & then
        mockMvc.perform(post("/api/predictions/races/" + predictableRace.getId())
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PR003"))
                .andDo(print());
    }

    @DisplayName("POST /api/predictions/races/{raceId} - 예측 제출 실패 (인증되지 않은 사용자)")
    @Test
    void createPrediction_Fail_Unauthorized() throws Exception {
        // given
        PredictionRequest request = new PredictionRequest(1, 2, 3);

        // when & then
        mockMvc.perform(post("/api/predictions/races/" + predictableRace.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andDo(print());
    }

    @DisplayName("GET /api/predictions/races/{raceId}/me - 내 예측 조회 성공")
    @Test
    void getMyPrediction_Success() throws Exception {
        // given: 먼저 예측을 하나 생성
        PredictionRequest request = new PredictionRequest(1, 2, 3);
        mockMvc.perform(post("/api/predictions/races/" + predictableRace.getId())
                .header("Authorization", userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // when & then
        mockMvc.perform(get("/api/predictions/races/" + predictableRace.getId() + "/me")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.raceId").value(predictableRace.getId()))
                .andExpect(jsonPath("$.predictedP1.driverNumber").value(1))
                .andDo(print());
    }

    @DisplayName("GET /api/predictions/leaderboard - 리더보드 조회 성공 (포인트 내림차순)")
    @Test
    void getLeaderboard_Success() throws Exception {
        mockMvc.perform(get("/api/predictions/leaderboard")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()", is(4)))
                .andExpect(jsonPath("$.content[0].rank").value(1))
                .andExpect(jsonPath("$.content[0].nickname").value("TopRanker"))
                .andExpect(jsonPath("$.content[0].points").value(100))
                .andExpect(jsonPath("$.content[1].rank").value(2))
                .andExpect(jsonPath("$.content[1].nickname").value("MidRanker"))
                .andExpect(jsonPath("$.content[3].nickname").value("user"))
                .andExpect(jsonPath("$.content[3].points").value(0))
                .andDo(print());
    }

    @DisplayName("GET /api/predictions/leaderboard - 페이징 파라미터 적용 확인")
    @Test
    void getLeaderboard_Paging_Success() throws Exception {
        mockMvc.perform(get("/api/predictions/leaderboard")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(2)))
                .andExpect(jsonPath("$.content[0].rank").value(3))
                .andExpect(jsonPath("$.content[0].nickname").value("LowRanker"))
                .andExpect(jsonPath("$.content[1].rank").value(4))
                .andExpect(jsonPath("$.content[1].nickname").value("user"))
                .andDo(print());
    }
}