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

    @BeforeEach
    void setUp() {
        predictionRepository.deleteAllInBatch();
        raceRepository.deleteAllInBatch();
        driverRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        testUser = userRepository.save(User.builder().email("test@test.com").password("pwd").nickname("testuser").role(UserRole.USER).build());
        userToken = jwtUtil.createToken(testUser.getEmail(), testUser.getRole());

        ZonedDateTime now = ZonedDateTime.now();
        Race race1 = raceRepository.save(Race.builder()
                .id(1L).year(now.getYear()).dateStart(now).dateEnd(now)
                .meetingKey(1L).meetingName("GP1").sessionName("Race")
                .countryName("C1").circuitShortName("C1")
                .build());
        Race race2 = raceRepository.save(Race.builder()
                .id(2L).year(now.getYear()).dateStart(now).dateEnd(now)
                .meetingKey(2L).meetingName("GP2").sessionName("Race")
                .countryName("C2").circuitShortName("C2")
                .build());
        Driver driver1 = driverRepository.save(Driver.builder().id(1).build());

        Prediction prediction1 = Prediction.builder().user(testUser).race(race1).predictedP1(driver1).predictedP2(driver1).predictedP3(driver1).build();
        prediction1.updateResult(true, 10);

        Prediction prediction2 = Prediction.builder().user(testUser).race(race2).predictedP1(driver1).predictedP2(driver1).predictedP3(driver1).build();

        predictionRepository.saveAll(List.of(prediction1, prediction2));
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

    @DisplayName("GET /api/users/me - 내 프로필 조회 시 예측 통계 정보 포함 확인")
    @Test
    void getMyProfile_WithPredictionStats_Success() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("testuser"))
                .andExpect(jsonPath("$.predictionStats").exists())
                .andExpect(jsonPath("$.predictionStats.totalPredictions").value(2))
                .andExpect(jsonPath("$.predictionStats.correctPredictions").value(1))
                .andExpect(jsonPath("$.predictionStats.winRate").value(0.5)) // 1 / 2 = 0.5
                .andDo(print());
    }
}