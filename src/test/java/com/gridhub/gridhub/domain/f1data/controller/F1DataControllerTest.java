package com.gridhub.gridhub.domain.f1data.controller;

import com.gridhub.gridhub.domain.f1data.entity.Race;
import com.gridhub.gridhub.domain.f1data.repository.RaceRepository;
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

    private final int TARGET_YEAR = 2024;

    @BeforeEach
    void setUp() {
        raceRepository.deleteAllInBatch();

        // 테스트용 데이터 DB에 미리 저장
        Race race1 = Race.builder().id(101L).year(TARGET_YEAR).meetingKey(1L).meetingName("GP1").sessionName("Race").countryName("Country1").circuitShortName("Circ1").dateStart(ZonedDateTime.now()).dateEnd(ZonedDateTime.now()).build();
        Race race2 = Race.builder().id(102L).year(TARGET_YEAR).meetingKey(1L).meetingName("GP1").sessionName("Qualifying").countryName("Country1").circuitShortName("Circ1").dateStart(ZonedDateTime.now()).dateEnd(ZonedDateTime.now()).build();
        Race race3 = Race.builder().id(103L).year(TARGET_YEAR - 1).meetingKey(2L).meetingName("GP2").sessionName("Race").countryName("Country2").circuitShortName("Circ2").dateStart(ZonedDateTime.now()).dateEnd(ZonedDateTime.now()).build();
        raceRepository.saveAll(List.of(race1, race2, race3));
    }

    @DisplayName("GET /api/f1-data/calendar - 특정 연도 캘린더 조회 성공")
    @Test
    void getRaceCalendar_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/f1-data/calendar")
                        .param("year", String.valueOf(TARGET_YEAR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray()) // 응답이 배열 형태인지
                .andExpect(jsonPath("$.length()").value(1)) // 2024년 미팅은 1개
                .andExpect(jsonPath("$[0].meetingKey").value(1L))
                .andExpect(jsonPath("$[0].meetingName").value("GP1"))
                .andExpect(jsonPath("$[0].sessions.length()").value(2)) // GP1의 세션은 2개
                .andDo(print());
    }

    @DisplayName("GET /api/f1-data/calendar - 연도 파라미터가 없을 경우 현재 연도로 조회")
    @Test
    void getRaceCalendar_Success_WhenYearParamIsMissing() throws Exception {
        // given
        // 현재 연도 데이터를 DB에 추가로 저장
        int currentYear = ZonedDateTime.now().getYear();
        Race currentYearRace = Race.builder().id(201L).year(currentYear).meetingKey(3L).meetingName("Current GP").sessionName("Race").countryName("Current").circuitShortName("CUR").dateStart(ZonedDateTime.now()).dateEnd(ZonedDateTime.now()).build();
        raceRepository.save(currentYearRace);

        // when & then
        mockMvc.perform(get("/api/f1-data/calendar")) // year 파라미터 없이 호출
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].meetingName").value("Current GP")) // 현재 연도의 데이터가 조회되는지 확인
                .andDo(print());
    }

    @DisplayName("GET /api/f1-data/calendar - 데이터가 없는 연도 조회 시 빈 배열 반환")
    @Test
    void getRaceCalendar_Success_WhenNoData() throws Exception {
        // when & then
        mockMvc.perform(get("/api/f1-data/calendar")
                        .param("year", "1999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0)) // 빈 배열을 반환하는지
                .andDo(print());
    }
}