package com.gridhub.gridhub.domain.f1data.service;

import com.gridhub.gridhub.domain.f1data.dto.RaceCalendarDto;
import com.gridhub.gridhub.domain.f1data.entity.Race;
import com.gridhub.gridhub.domain.f1data.repository.RaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class F1DataServiceTest {

    @InjectMocks
    private F1DataService f1DataService;

    @Mock
    private RaceRepository raceRepository;

    @DisplayName("연도별 레이스 캘린더 조회 - 단위 테스트")
    @Test
    void getRaceCalendarByYear_Unit_Test() {
        // given
        int year = 2024;

        // Mock 데이터 생성 (2개의 미팅, 총 3개의 세션)
        Race race1_session1 = Race.builder().meetingKey(1L).meetingName("GP1").sessionName("Qualifying").dateStart(ZonedDateTime.now()).build();
        Race race1_session2 = Race.builder().meetingKey(1L).meetingName("GP1").sessionName("Race").dateStart(ZonedDateTime.now().plusDays(1)).build();
        Race race2_session1 = Race.builder().meetingKey(2L).meetingName("GP2").sessionName("Race").dateStart(ZonedDateTime.now().plusDays(7)).build();

        List<Race> mockRaces = List.of(race1_session1, race1_session2, race2_session1);

        // Mock Repository 설정
        given(raceRepository.findByYearOrderByDateStartAsc(year)).willReturn(mockRaces);

        // when
        List<RaceCalendarDto> result = f1DataService.getRaceCalendarByYear(year);

        // then
        // 1. 총 2개의 그랑프리(Meeting)가 반환되었는지 확인
        assertThat(result).hasSize(2);

        // 2. 첫 번째 그랑프리(GP1)가 2개의 세션을 포함하는지 확인
        RaceCalendarDto gp1 = result.stream()
                .filter(dto -> dto.meetingKey().equals(1L))
                .findFirst()
                .orElseThrow();

        assertThat(gp1.meetingName()).isEqualTo("GP1");
        assertThat(gp1.sessions()).hasSize(2);
        assertThat(gp1.sessions().get(0).sessionName()).isEqualTo("Qualifying");
        assertThat(gp1.sessions().get(1).sessionName()).isEqualTo("Race");

        // 3. 두 번째 그랑프리(GP2)가 1개의 세션을 포함하는지 확인
        RaceCalendarDto gp2 = result.stream()
                .filter(dto -> dto.meetingKey().equals(2L))
                .findFirst()
                .orElseThrow();

        assertThat(gp2.meetingName()).isEqualTo("GP2");
        assertThat(gp2.sessions()).hasSize(1);
    }
}