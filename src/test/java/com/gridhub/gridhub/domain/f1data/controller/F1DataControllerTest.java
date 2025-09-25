package com.gridhub.gridhub.domain.f1data.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridhub.gridhub.domain.f1data.entity.*;
import com.gridhub.gridhub.domain.f1data.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class F1DataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RaceRepository raceRepository;
    @Autowired
    private RaceResultRepository raceResultRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private DriverRepository driverRepository;
    @Autowired
    private PositionRepository positionRepository;

    private Race testRaceWithResult;
    private Race testRaceWithoutResult;

    @BeforeEach
    void setUp() {
        // 테스트 실행 전 모든 관련 테이블 초기화
        positionRepository.deleteAllInBatch();
        raceResultRepository.deleteAllInBatch();
        raceRepository.deleteAllInBatch();
        driverRepository.deleteAllInBatch();
        teamRepository.deleteAllInBatch();

        // 테스트용 데이터 DB에 미리 저장
        Team team = teamRepository.save(Team.builder().name("Red Bull").teamColour("1E41FF").build());
        Driver driver = driverRepository.save(Driver.builder().id(1).fullName("M. Verstappen").team(team).build());

        testRaceWithResult = Race.builder()
                .id(101L).year(2024).meetingKey(1L).meetingName("GP1 with Result").sessionName("Race")
                .countryName("Country1").circuitShortName("Circ1").dateStart(ZonedDateTime.now()).dateEnd(ZonedDateTime.now())
                .build();
        raceRepository.save(testRaceWithResult);

        // 연관관계 편의 메서드를 사용하여 객체 그래프의 일관성을 유지
        RaceResult result = RaceResult.builder().race(testRaceWithResult).build();
        Position position = Position.builder().driver(driver).racePosition(1).build();
        result.addPosition(position); // RaceResult에 Position 추가
        raceResultRepository.save(result); // RaceResult만 저장하면 Position도 함께 저장됨

        testRaceWithoutResult = Race.builder()
                .id(102L).year(2024).meetingKey(2L).meetingName("GP2 without Result").sessionName("Race")
                .countryName("Country2").circuitShortName("Circ2").dateStart(ZonedDateTime.now()).dateEnd(ZonedDateTime.now())
                .build();
        raceRepository.save(testRaceWithoutResult);
    }

    @DisplayName("GET /api/f1-data/calendar - 특정 연도 캘린더 조회 성공")
    @Test
    void getRaceCalendar_Success() throws Exception {
        mockMvc.perform(get("/api/f1-data/calendar")
                        .param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andDo(print());
    }

    @DisplayName("GET /api/f1-data/races/{raceId} - 특정 레이스 상세 정보 조회 성공")
    @Test
    void getRaceDetail_Success() throws Exception {
        mockMvc.perform(get("/api/f1-data/races/" + testRaceWithResult.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionKey").value(testRaceWithResult.getId()))
                .andExpect(jsonPath("$.positions.length()").value(1))
                .andExpect(jsonPath("$.positions[0].driverFullName").value("M. Verstappen"))
                .andDo(print());
    }

    @DisplayName("GET /api/f1-data/races/{raceId} - 존재하지 않는 레이스 조회 시 404 Not Found 응답")
    @Test
    void getRaceDetail_Fail_WhenRaceNotFound() throws Exception {
        long nonExistentRaceId = 9999L;

        mockMvc.perform(get("/api/f1-data/races/" + nonExistentRaceId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("F001"))
                .andDo(print());
    }

    @DisplayName("GET /api/f1-data/races/{raceId} - 결과가 없는 레이스 조회 시 404 Not Found 응답")
    @Test
    void getRaceDetail_Fail_WhenRaceResultNotFound() throws Exception {
        mockMvc.perform(get("/api/f1-data/races/" + testRaceWithoutResult.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("F002"))
                .andDo(print());
    }
}