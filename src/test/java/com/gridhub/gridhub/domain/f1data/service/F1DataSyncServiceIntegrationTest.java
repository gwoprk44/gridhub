package com.gridhub.gridhub.domain.f1data.service;

import com.gridhub.gridhub.domain.f1data.dto.MeetingResponse;
import com.gridhub.gridhub.domain.f1data.dto.PositionResponse;
import com.gridhub.gridhub.domain.f1data.dto.SessionResponse;
import com.gridhub.gridhub.domain.f1data.entity.Race;
import com.gridhub.gridhub.domain.f1data.repository.DriverRepository;
import com.gridhub.gridhub.domain.f1data.repository.RaceRepository;
import com.gridhub.gridhub.domain.f1data.repository.RaceResultRepository;
import com.gridhub.gridhub.infra.external.OpenF1Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@Transactional
class F1DataSyncServiceIntegrationTest {

    @Autowired
    private F1DataSyncService f1DataSyncService;

    @Autowired
    private RaceRepository raceRepository;
    @Autowired
    private RaceResultRepository raceResultRepository;
    @Autowired
    private DriverRepository driverRepository;

    @MockitoBean
    private OpenF1Client openF1Client;

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 DB를 깨끗하게 비웁니다.
        raceResultRepository.deleteAllInBatch();
        raceRepository.deleteAllInBatch();
        driverRepository.deleteAllInBatch();
    }

    @DisplayName("통합 테스트: 종료된 레이스의 결과가 DB에 정상적으로 저장된다")
    @Test
    void synchronizeF1Data_IntegrationTest() {
        // given
        MeetingResponse meeting = new MeetingResponse(1L, "Test GP", "Testland", "TST", 2024);
        SessionResponse raceSession = new SessionResponse(101L, 1L, "Race", ZonedDateTime.now().minusDays(2), ZonedDateTime.now().minusDays(1), 2024);

        given(openF1Client.getMeetingsByYear(anyInt())).willReturn(List.of(meeting));
        given(openF1Client.getSessionsByMeeting(1L)).willReturn(List.of(raceSession));
        given(openF1Client.getRaceResult(101L)).willReturn(List.of(new PositionResponse(44, 1, "date", "Finished")));

        given(openF1Client.getDriversBySession(anyLong())).willReturn(Collections.emptyList());
        given(openF1Client.getQualifyingResult(anyLong())).willReturn(Collections.emptyList());
        given(openF1Client.getWeather(anyLong())).willReturn(Collections.emptyList());
        given(openF1Client.getRaceControlMessages(anyLong())).willReturn(Collections.emptyList());

        // when
        f1DataSyncService.synchronizeF1Data();

        // then
        Race savedRace = raceRepository.findById(101L).orElseThrow();

        assertThat(savedRace).isNotNull();
        assertThat(savedRace.getMeetingName()).isEqualTo("Test GP");
        // RaceResult가 Cascade에 의해 함께 저장되었는지 확인
        assertThat(savedRace.getRaceResult()).isNotNull();
        assertThat(savedRace.getRaceResult().getId()).isNotNull();

        // RaceResultRepository를 통해서도 조회가 가능한지 확인
        boolean resultExists = raceResultRepository.existsById(savedRace.getRaceResult().getId());
        assertThat(resultExists).isTrue();
    }
}