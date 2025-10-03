package com.gridhub.gridhub.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridhub.gridhub.domain.f1data.entity.Driver;
import com.gridhub.gridhub.domain.f1data.entity.Race;
import com.gridhub.gridhub.domain.f1data.repository.DriverRepository;
import com.gridhub.gridhub.domain.f1data.repository.RaceRepository;
import com.gridhub.gridhub.domain.prediction.entity.Prediction;
import com.gridhub.gridhub.domain.prediction.repository.PredictionRepository;
import com.gridhub.gridhub.domain.user.dto.ProfileUpdateRequest;
import com.gridhub.gridhub.domain.user.entity.User;
import com.gridhub.gridhub.domain.user.entity.UserRole;
import com.gridhub.gridhub.domain.user.repository.UserRepository;
import com.gridhub.gridhub.global.util.JwtUtil;
import com.gridhub.gridhub.infra.s3.S3UploaderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.List;
import java.time.LocalDateTime;

import static org.springframework.test.util.ReflectionTestUtils.setField;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private PredictionRepository predictionRepository;
    @Autowired
    private RaceRepository raceRepository;
    @Autowired
    private DriverRepository driverRepository;


    @MockitoBean
    private S3UploaderService s3UploaderService;

    private String userToken;
    private User testUser;
    private User anotherUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder().email("test@test.com").password("pwd").nickname("testuser").role(UserRole.USER).build();
        userRepository.saveAndFlush(testUser);
        userToken = jwtUtil.createToken(testUser.getEmail(), testUser.getRole());
        anotherUser = userRepository.save(User.builder().email("another@test.com").password("pwd").nickname("anotherUser").role(UserRole.USER).build());
        userToken = jwtUtil.createToken(testUser.getEmail(), testUser.getRole());
    }

    @DisplayName("GET /api/users/me - 내 프로필 조회 성공")
    @Test
    void getMyProfile_Success() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@test.com"))
                .andDo(print());
    }

    @DisplayName("PATCH /api/users/me - 내 프로필 수정 성공 (이미지 포함)")
    @Test
    void updateMyProfile_Success() throws Exception {
        // given
        ProfileUpdateRequest requestDto = new ProfileUpdateRequest();
        requestDto.setNickname("newNick"); // "updatedNickname" (15자) -> "newNick" (7자)
        requestDto.setBio("My new bio");

        MockMultipartFile jsonRequest = new MockMultipartFile(
                "request", "", "application/json",
                objectMapper.writeValueAsString(requestDto).getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile imageFile = new MockMultipartFile(
                "image", "profile.jpg", "image/jpeg", "image content".getBytes()
        );

        String fakeImageUrl = "https://s3.com/profile.jpg";
        given(s3UploaderService.upload(any(MockMultipartFile.class))).willReturn(fakeImageUrl);

        // when & then
        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/me")
                        .file(jsonRequest)
                        .file(imageFile)
                        .header("Authorization", userToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andDo(print());

        // 수정된 프로필 정보를 다시 조회하여 검증
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("newNick"))
                .andExpect(jsonPath("$.bio").value("My new bio"))
                .andExpect(jsonPath("$.profileImageUrl").value(fakeImageUrl));
    }

    @DisplayName("PATCH /api/users/me - 닉네임 중복 시 400 Bad Request 응답")
    @Test
    void updateMyProfile_Fail_NicknameAlreadyExists() throws Exception {
        // given
        userRepository.save(User.builder().email("other@test.com").password("pwd").nickname("newNick").role(UserRole.USER).build());

        ProfileUpdateRequest requestDto = new ProfileUpdateRequest();
        requestDto.setNickname("newNick");

        MockMultipartFile jsonRequest = new MockMultipartFile(
                "request", "", "application/json",
                objectMapper.writeValueAsString(requestDto).getBytes(StandardCharsets.UTF_8)
        );

        // when & then
        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/me")
                        .file(jsonRequest)
                        .header("Authorization", userToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("U002"))
                .andDo(print());
    }

    @DisplayName("GET /api/users/me - 내 프로필 조회 시 티어 정보 포함 확인")
    @Test
    void getMyProfile_WithTier_Success() throws Exception {
        // given
        // testUser의 포인트를 1600점으로 설정 (Platinum 티어 예상)
        testUser.addPoints(1600);
        userRepository.save(testUser);

        // when & then
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("testuser"))
                .andExpect(jsonPath("$.points").value(1600))
                .andExpect(jsonPath("$.tier").value("Platinum"))
                .andDo(print());
    }

    @DisplayName("GET /api/users/me - 내 프로필 조회 시 예측 통계 및 최근 기록 포함 확인")
    @Test
    void getMyProfile_WithPredictionData_Success() throws Exception {
        // given: 이 테스트에만 필요한 예측 관련 데이터를 여기서 생성
        Race race1 = raceRepository.save(createTestRace(1L, "GP1"));
        Race race2 = raceRepository.save(createTestRace(2L, "GP2"));
        Driver driver1 = driverRepository.save(Driver.builder().id(1).build());

        Prediction p1 = Prediction.builder().user(testUser).race(race1).predictedP1(driver1).predictedP2(driver1).predictedP3(driver1).build();
        setField(p1, "createdAt", LocalDateTime.now().minusDays(2));
        p1.updateResult(true, 10); // 성공한 예측으로 설정

        Prediction p2 = Prediction.builder().user(testUser).race(race2).predictedP1(driver1).predictedP2(driver1).predictedP3(driver1).build();
        setField(p2, "createdAt", LocalDateTime.now()); // 실패한 예측 (가장 최신)

        predictionRepository.saveAllAndFlush(List.of(p1, p2));

        // when & then
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                // 1. 예측 통계 검증
                .andExpect(jsonPath("$.predictionStats.totalPredictions").value(2))
                .andExpect(jsonPath("$.predictionStats.correctPredictions").value(1))
                .andExpect(jsonPath("$.predictionStats.winRate").value(0.5))
                // 2. 최근 예측 기록 검증
                .andExpect(jsonPath("$.recentPredictions.length()").value(2))
                .andExpect(jsonPath("$.recentPredictions[0].raceName").value("GP2")) // 최신 예측이 먼저 오는지 확인
                .andDo(print());
    }

    // 테스트용 Race 객체를 생성하는 헬퍼 메서드
    private Race createTestRace(Long id, String meetingName) {
        return Race.builder()
                .id(id)
                .year(ZonedDateTime.now().getYear())
                .dateStart(ZonedDateTime.now())
                .dateEnd(ZonedDateTime.now())
                .meetingKey(id)
                .meetingName(meetingName)
                .sessionName("Race")
                .countryName("Testland")
                .circuitShortName("TST")
                .build();
    }

    @DisplayName("GET /api/users/{userId} - ID로 다른 사용자 프로필 조회 성공")
    @Test
    void getUserProfileById_Success() throws Exception {
        mockMvc.perform(get("/api/users/" + anotherUser.getId())) // anotherUser의 ID로 조회
                // 이 API는 비로그인 상태에서도 조회가 가능해야 하므로 header(Authorization)가 없어도 성공해야 함
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("anotherUser"))
                .andExpect(jsonPath("$.email").value("another@test.com"))
                .andDo(print());
    }

    @DisplayName("GET /api/users/by-nickname/{nickname} - 닉네임으로 다른 사용자 프로필 조회 성공")
    @Test
    void getUserProfileByNickname_Success() throws Exception {
        mockMvc.perform(get("/api/users/by-nickname/" + anotherUser.getNickname())) // anotherUser의 닉네임으로 조회
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("anotherUser"))
                .andDo(print());
    }

    @DisplayName("GET /api/users/{userId} - 존재하지 않는 ID로 프로필 조회 시 404 Not Found 응답")
    @Test
    void getUserProfileById_Fail_UserNotFound() throws Exception {
        long nonExistentUserId = 9999L;

        mockMvc.perform(get("/api/users/" + nonExistentUserId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("U004")) // UserNotFoundException에 해당하는 에러 코드
                .andDo(print());
    }
}