package com.gridhub.gridhub.domain.f1data.service;

import com.gridhub.gridhub.domain.f1data.dto.MeetingResponse;
import com.gridhub.gridhub.domain.f1data.dto.SessionResponse;
import com.gridhub.gridhub.domain.f1data.entity.Race;
import com.gridhub.gridhub.domain.f1data.entity.RaceResult;
import com.gridhub.gridhub.domain.f1data.repository.*;
import com.gridhub.gridhub.infra.external.OpenF1Client;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class F1DataSyncServiceTest {

    @InjectMocks
    private F1DataSyncService f1DataSyncService;

    @Mock
    private OpenF1Client openF1Client;
    @Mock
    private RaceRepository raceRepository;
    @Mock
    private RaceResultRepository raceResultRepository;
    @Mock
    private DriverRepository driverRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private PositionRepository positionRepository;
    @Mock
    private RaceControlRepository raceControlRepository;

    @DisplayName("종료된 레이스 + 결과 데이터가 없으면 -> 결과 동기화를 수행한다")
    @Test
    void whenRaceIsFinishedAndResultIsMissing_thenSynchronizeResult() {
        // given
        MeetingResponse meeting = new MeetingResponse(1L, "Test GP", "Country", "Circ", 2024);
        SessionResponse raceSession = new SessionResponse(101L, 1L, "Race", ZonedDateTime.now().minusDays(2), ZonedDateTime.now().minusDays(1), 2024);
        Race raceWithoutResult = Race.builder().id(101L).build();

        given(openF1Client.getMeetingsByYear(anyInt())).willReturn(List.of(meeting));
        given(openF1Client.getSessionsByMeeting(1L)).willReturn(List.of(raceSession));
        given(openF1Client.getDriversBySession(anyLong())).willReturn(Collections.emptyList()); // 빈 리스트 반환

        given(raceRepository.findById(101L)).willReturn(Optional.of(raceWithoutResult));

        // when
        f1DataSyncService.synchronizeF1Data();

        // then
        verify(raceResultRepository, times(1)).save(any(RaceResult.class));
    }

    @DisplayName("아직 종료되지 않은 레이스는 -> 결과 동기화를 수행하지 않는다")
    @Test
    void whenRaceIsNotFinished_thenSkipSynchronization() {
        // given
        MeetingResponse meeting = new MeetingResponse(1L, "Upcoming GP", "Country", "Circ", 2024);
        SessionResponse raceSession = new SessionResponse(101L, 1L, "Race", ZonedDateTime.now().plusDays(1), ZonedDateTime.now().plusDays(2), 2024);

        given(openF1Client.getMeetingsByYear(anyInt())).willReturn(List.of(meeting));
        given(openF1Client.getSessionsByMeeting(1L)).willReturn(List.of(raceSession));
        given(openF1Client.getDriversBySession(anyLong())).willReturn(Collections.emptyList());

        // when
        f1DataSyncService.synchronizeF1Data();

        // then
        verify(raceResultRepository, never()).save(any(RaceResult.class));
    }

    @DisplayName("이미 결과 데이터가 있는 레이스는 -> 결과 동기화를 수행하지 않는다")
    @Test
    void whenRaceResultExists_thenSkipSynchronization() {
        // given
        MeetingResponse meeting = new MeetingResponse(1L, "Finished GP", "Country", "Circ", 2024);
        SessionResponse raceSession = new SessionResponse(101L, 1L, "Race", ZonedDateTime.now().minusDays(2), ZonedDateTime.now().minusDays(1), 2024);
        Race raceWithResult = Race.builder().id(101L).build();
        RaceResult mockResult = mock(RaceResult.class);
        raceWithResult.setRaceResult(mockResult);

        given(openF1Client.getMeetingsByYear(anyInt())).willReturn(List.of(meeting));
        given(openF1Client.getSessionsByMeeting(1L)).willReturn(List.of(raceSession));
        // synchronizeDriversAndTeams 내부에서 호출되는 API도 Mocking 추가
        given(openF1Client.getDriversBySession(anyLong())).willReturn(Collections.emptyList());

        given(raceRepository.findById(101L)).willReturn(Optional.of(raceWithResult));

        // when
        f1DataSyncService.synchronizeF1Data();

        // then
        verify(raceResultRepository, never()).save(any(RaceResult.class));
    }
}