package com.gridhub.gridhub.domain.f1data.service;

import com.gridhub.gridhub.domain.f1data.dto.DriverInfoResponse;
import com.gridhub.gridhub.domain.f1data.dto.RaceCalendarDto;
import com.gridhub.gridhub.domain.f1data.dto.RaceDetailResponse;
import com.gridhub.gridhub.domain.f1data.dto.TeamInfoResponse;
import com.gridhub.gridhub.domain.f1data.entity.*;
import com.gridhub.gridhub.domain.f1data.exception.RaceResultNotFoundException;
import com.gridhub.gridhub.domain.f1data.repository.DriverRepository;
import com.gridhub.gridhub.domain.f1data.repository.RaceRepository;
import com.gridhub.gridhub.domain.f1data.repository.RaceResultRepository;
import com.gridhub.gridhub.domain.f1data.repository.TeamRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class F1DataServiceTest {

    @InjectMocks
    private F1DataService f1DataService;

    @Mock
    private RaceRepository raceRepository;

    @Mock
    private RaceResultRepository raceResultRepository;

    @Mock
    private DriverRepository driverRepository;
    @Mock
    private TeamRepository teamRepository;

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

    @DisplayName("특정 레이스 상세 정보 조회 - 단위 테스트")
    @Test
    void getRaceDetail_Unit_Test() {
        // given
        Long raceId = 101L;

        // Mock 엔티티 설정
        Race mockRace = Race.builder().id(raceId).meetingName("Test GP").sessionName("Race").build();
        RaceResult mockResult = RaceResult.builder().race(mockRace).latestWeather(Collections.emptyMap()).build();

        Team mockTeam = Team.builder().name("Mercedes").teamColour("00D2BE").build();
        Driver mockDriver = Driver.builder().id(44).fullName("L. Hamilton").team(mockTeam).build();
        Position mockPosition = Position.builder().driver(mockDriver).racePosition(1).build();
        mockResult.addPosition(mockPosition); // 연관관계 설정

        // Mock Repository 설정
        given(raceRepository.findById(raceId)).willReturn(Optional.of(mockRace));
        given(raceResultRepository.findByRaceWithDetails(mockRace)).willReturn(Optional.of(mockResult));

        // when
        RaceDetailResponse result = f1DataService.getRaceDetail(raceId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.sessionKey()).isEqualTo(raceId);
        assertThat(result.meetingName()).isEqualTo("Test GP");
        assertThat(result.positions()).hasSize(1);
        assertThat(result.positions().get(0).driverNumber()).isEqualTo(44);
        assertThat(result.positions().get(0).racePosition()).isEqualTo(1);
        assertThat(result.positions().get(0).driverTeamName()).isEqualTo("Mercedes");
    }

    @DisplayName("특정 레이스 상세 정보 조회 실패 - 결과 없음")
    @Test
    void getRaceDetail_Fail_WhenResultNotFound() {
        // given
        Long raceId = 101L;
        Race mockRace = Race.builder().id(raceId).build();

        given(raceRepository.findById(raceId)).willReturn(Optional.of(mockRace));
        given(raceResultRepository.findByRaceWithDetails(mockRace)).willReturn(Optional.empty()); // 결과가 없도록 설정

        // when & then
        assertThrows(RaceResultNotFoundException.class, () -> f1DataService.getRaceDetail(raceId));
    }

    @DisplayName("모든 드라이버 목록 조회 - 단위 테스트")
    @Test
    void getAllDrivers_Unit_Test() {
        // given
        Team mockTeam = Team.builder().name("Mercedes").teamColour("00D2BE").build();
        Driver mockDriver1 = Driver.builder().id(44).fullName("L. Hamilton").team(mockTeam).build();
        Driver mockDriver2 = Driver.builder().id(63).fullName("G. Russell").team(mockTeam).build();
        List<Driver> mockDrivers = List.of(mockDriver1, mockDriver2);

        given(driverRepository.findAll()).willReturn(mockDrivers);

        // when
        List<DriverInfoResponse> result = f1DataService.getAllDrivers();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).driverNumber()).isEqualTo(44);
        assertThat(result.get(0).teamName()).isEqualTo("Mercedes");
        assertThat(result.get(1).fullName()).isEqualTo("G. Russell");
    }

    @DisplayName("모든 팀 목록 조회 (소속 드라이버 포함) - 단위 테스트")
    @Test
    void getAllTeams_Unit_Test() {
        // given
        Team mockTeam = Team.builder().name("Mercedes").teamColour("00D2BE").build();
        Driver mockDriver1 = Driver.builder().id(44).fullName("L. Hamilton").team(mockTeam).build();
        Driver mockDriver2 = Driver.builder().id(63).fullName("G. Russell").team(mockTeam).build();
        mockTeam.getDrivers().addAll(List.of(mockDriver1, mockDriver2)); // 연관관계 설정

        List<Team> mockTeams = List.of(mockTeam);

        given(teamRepository.findAllWithDrivers()).willReturn(mockTeams);

        // when
        List<TeamInfoResponse> result = f1DataService.getAllTeams();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).teamName()).isEqualTo("Mercedes");
        assertThat(result.get(0).drivers()).hasSize(2);
        assertThat(result.get(0).drivers().get(0).driverNumber()).isEqualTo(44);
        assertThat(result.get(0).drivers().get(1).fullName()).isEqualTo("G. Russell");
    }
}