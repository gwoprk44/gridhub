package com.gridhub.gridhub.domain.f1data.controller;

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

    // 데이터 준비를 위한 Repository 주입
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
        // 테이블 초기화
        positionRepository.deleteAllInBatch();
        raceResultRepository.deleteAllInBatch();
        raceRepository.deleteAllInBatch();
        driverRepository.deleteAllInBatch();
        teamRepository.deleteAllInBatch();

        // 팀 생성
        Team redbullTeam = teamRepository.save(Team.builder().name("Red Bull Racing").teamColour("0600EF").build());
        Team ferrariTeam = teamRepository.save(Team.builder().name("Ferrari").teamColour("DC0000").build());

        // 드라이버 생성 (수정된 빌더가 자동으로 양방향 관계를 설정해 줌)
        Driver driver1 = driverRepository.save(Driver.builder().id(1).fullName("M. Verstappen").team(redbullTeam).build());
        driverRepository.save(Driver.builder().id(16).fullName("C. Leclerc").team(ferrariTeam).build());
        driverRepository.save(Driver.builder().id(55).fullName("C. Sainz").team(ferrariTeam).build());

        // 결과가 있는 레이스 데이터 생성
        testRaceWithResult = raceRepository.save(Race.builder()
                .id(101L).year(2024).meetingKey(1L).meetingName("GP1 with Result").sessionName("Race")
                .countryName("Country1").circuitShortName("Circ1").dateStart(ZonedDateTime.now()).dateEnd(ZonedDateTime.now())
                .build());

        RaceResult result = RaceResult.builder().race(testRaceWithResult).build();
        Position position = Position.builder().driver(driver1).racePosition(1).build();
        result.addPosition(position);
        raceResultRepository.save(result);

        // 결과가 없는 레이스 데이터 생성
        testRaceWithoutResult = raceRepository.save(Race.builder()
                .id(102L).year(2024).meetingKey(2L).meetingName("GP2 without Result").sessionName("Race")
                .countryName("Country2").circuitShortName("Circ2").dateStart(ZonedDateTime.now()).dateEnd(ZonedDateTime.now())
                .build());
    }

    @DisplayName("GET /api/f1-data/calendar - 특정 연도 캘린더 조회 성공")
    @Test
    void getRaceCalendar_Success() throws Exception {
        mockMvc.perform(get("/api/f1-data/calendar")
                        .param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2)) // 2024년 미팅은 2개 (GP1, GP2)
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

    @DisplayName("GET /api/f1-data/drivers - 모든 드라이버 목록 조회 성공")
    @Test
    void getAllDrivers_Success() throws Exception {
        mockMvc.perform(get("/api/f1-data/drivers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3)) // 총 3명의 드라이버
                .andExpect(jsonPath("$[?(@.driverNumber == 16)].fullName").value("C. Leclerc"))
                .andDo(print());
    }

    @DisplayName("GET /api/f1-data/teams - 모든 팀 목록 및 소속 드라이버 조회 성공")
    @Test
    void getAllTeams_Success() throws Exception {
        mockMvc.perform(get("/api/f1-data/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2)) // Red Bull, Ferrari
                .andExpect(jsonPath("$[?(@.teamName == 'Ferrari')].drivers.length()").value(2))
                .andExpect(jsonPath("$[?(@.teamName == 'Ferrari')].drivers[?(@.driverNumber == 55)].fullName").value("C. Sainz"))
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