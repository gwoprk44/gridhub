package com.gridhub.gridhub.domain.f1data.service;

import com.gridhub.gridhub.domain.f1data.dto.DriverResponse;
import com.gridhub.gridhub.domain.f1data.dto.MeetingResponse;
import com.gridhub.gridhub.domain.f1data.dto.PositionResponse;
import com.gridhub.gridhub.domain.f1data.dto.SessionResponse;
import com.gridhub.gridhub.domain.f1data.entity.*;
import com.gridhub.gridhub.domain.f1data.repository.*;
import com.gridhub.gridhub.infra.external.OpenF1Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Open F1 API로부터 F1 관련 데이터를 주기적으로 가져와 DB에 동기화하는 스케줄링 서비스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class F1DataSyncService {

    private final OpenF1Client openF1Client;
    private final RaceRepository raceRepository;
    private final DriverRepository driverRepository;
    private final TeamRepository teamRepository;
    private final RaceResultRepository raceResultRepository;
    private final PositionRepository positionRepository;
    private final RaceControlRepository raceControlRepository;

    /**
     * 매일 새벽 5시에 F1 데이터 동기화를 실행합니다.
     */
    @Scheduled(cron = "0 0 5 * * *")
    @Transactional
    public void synchronizeF1Data() {
        log.info("F1 데이터 동기화 스케줄을 시작합니다.");
        int currentYear = Year.now().getValue();

        // 1. 드라이버 및 팀 정보 동기화 (시즌 중 라인업 변경이 있을 수 있으므로 주기적으로 실행)
        synchronizeDriversAndTeams(currentYear);

        // 2. 레이스 일정(Meeting, Session) 동기화
        List<MeetingResponse> meetings = openF1Client.getMeetingsByYear(currentYear);
        synchronizeRaceSchedule(meetings);

        // 3. 종료된 레이스의 '결과' 데이터 동기화
        synchronizeFinishedRaceResults(meetings);

        log.info("F1 데이터 동기화 스케줄을 종료합니다.");
    }

    private void synchronizeDriversAndTeams(int year) {
        // ... (이전 코드와 동일하게 유지)
        // 가장 최근 레이스 세션을 기준으로 드라이버/팀 정보를 가져옴
        Optional<SessionResponse> latestSessionOpt = openF1Client.getMeetingsByYear(year).stream()
                .findFirst()
                .flatMap(meeting -> openF1Client.getSessionsByMeeting(meeting.meetingKey()).stream()
                        .max(Comparator.comparing(SessionResponse::dateStart)));

        if (latestSessionOpt.isEmpty()) {
            log.warn("{}년도 세션 정보가 없어 드라이버/팀 동기화를 건너<binary data, 1 bytes>니다.", year);
            return;
        }

        List<DriverResponse> driverDtos = openF1Client.getDriversBySession(latestSessionOpt.get().sessionKey());

        Map<String, Team> teamsMap = driverDtos.stream()
                .map(dto -> new Team(dto.teamName(), dto.teamColour()))
                .distinct()
                .collect(Collectors.toMap(Team::getName, Function.identity(), (e1, e2) -> e1));

        teamRepository.saveAll(teamsMap.values());
        log.info("{}개의 팀 정보 동기화 완료.", teamsMap.size());

        List<Driver> drivers = driverDtos.stream()
                .map(dto -> {
                    Team team = teamsMap.get(dto.teamName());
                    return Driver.builder()
                            .id(dto.driverNumber())
                            .fullName(dto.fullName())
                            .headshotUrl(dto.headshotUrl())
                            .countryCode(dto.countryCode())
                            .team(team)
                            .build();
                })
                .toList();

        driverRepository.saveAll(drivers);
        log.info("{}명의 드라이버 정보 동기화 완료.", drivers.size());
    }

    private void synchronizeRaceSchedule(List<MeetingResponse> meetingDtos) {
        List<Race> racesToSave = new ArrayList<>();
        for (MeetingResponse meetingDto : meetingDtos) {
            List<SessionResponse> sessionDtos = openF1Client.getSessionsByMeeting(meetingDto.meetingKey());
            sessionDtos.forEach(sessionDto -> {
                Race race = Race.builder()
                        .id(sessionDto.sessionKey())
                        .sessionName(sessionDto.sessionName())
                        .dateStart(sessionDto.dateStart())
                        .dateEnd(sessionDto.dateEnd())
                        .meetingKey(meetingDto.meetingKey())
                        .meetingName(meetingDto.meetingName())
                        .countryName(meetingDto.countryName())
                        .circuitShortName(meetingDto.circuitShortName())
                        .year(meetingDto.year())
                        .build();
                racesToSave.add(race);
            });
        }
        raceRepository.saveAll(racesToSave);
        log.info("{}개의 세션(레이스) 일정 동기화 완료.", racesToSave.size());
    }

    private void synchronizeFinishedRaceResults(List<MeetingResponse> meetings) {
        log.info("종료된 레이스 결과 데이터 동기화를 시작합니다.");
        ZonedDateTime now = ZonedDateTime.now();
        Map<Integer, Driver> driverMap = driverRepository.findAll().stream()
                .collect(Collectors.toMap(Driver::getId, Function.identity()));

        for (MeetingResponse meeting : meetings) {
            List<SessionResponse> sessions = openF1Client.getSessionsByMeeting(meeting.meetingKey());

            sessions.stream()
                    .filter(session -> "Race".equalsIgnoreCase(session.sessionName()))
                    .filter(session -> session.dateEnd().isBefore(now))
                    .findFirst()
                    .ifPresent(raceSession -> {
                        Optional<Race> raceEntityOpt = raceRepository.findById(raceSession.sessionKey());
                        if (raceEntityOpt.isPresent() && raceEntityOpt.get().getRaceResult() == null) {
                            Race race = raceEntityOpt.get();
                            log.info("'{}' 결과 데이터 동기화 시작.", race.getMeetingName());

                            RaceResult raceResult = createAndSaveRaceResult(race, sessions, driverMap);
                            raceResultRepository.save(raceResult);
                        }
                    });
        }
        log.info("종료된 레이스 결과 데이터 동기화를 종료합니다.");
    }

    private RaceResult createAndSaveRaceResult(Race race, List<SessionResponse> sessions, Map<Integer, Driver> driverMap) {
        // 1. RaceResult 엔티티 생성
        Map<String, Object> latestWeather = findLatestWeather(race.getId());
        RaceResult raceResult = RaceResult.builder().race(race).latestWeather(latestWeather).build();

        // 2. 퀄리파잉 및 레이스 결과 Position 엔티티 생성
        Map<Integer, Integer> qualifyingResults = findAndFetchQualifyingResults(sessions);
        Map<Integer, Integer> raceResults = openF1Client.getRaceResult(race.getId()).stream()
                .collect(Collectors.toMap(PositionResponse::driverNumber, PositionResponse::position));

        raceResults.keySet().forEach(driverNumber -> {
            Driver driver = driverMap.get(driverNumber);
            if (driver != null) {
                Position position = Position.builder()
                        .driver(driver)
                        .qualifyingPosition(qualifyingResults.get(driverNumber))
                        .racePosition(raceResults.get(driverNumber))
                        .build();
                raceResult.addPosition(position);
            }
        });

        // 3. RaceControl 메시지 엔티티 생성
        openF1Client.getRaceControlMessages(race.getId()).forEach(rc -> {
            RaceControl raceControl = RaceControl.builder()
                    .date(rc.date())
                    .message(rc.message())
                    .flag(rc.flag() != null ? rc.flag() : "")
                    .build();
            raceResult.addRaceControl(raceControl);
        });

        return raceResult;
    }

    private Map<Integer, Integer> findAndFetchQualifyingResults(List<SessionResponse> sessions) {
        return sessions.stream()
                .filter(s -> s.sessionName().toLowerCase().contains("qualifying"))
                .max(Comparator.comparing(SessionResponse::dateStart))
                .map(qualifyingSession -> openF1Client.getQualifyingResult(qualifyingSession.sessionKey()).stream()
                        .collect(Collectors.toMap(PositionResponse::driverNumber, PositionResponse::position)))
                .orElse(Collections.emptyMap());
    }

    private Map<String, Object> findLatestWeather(long raceSessionKey) {
        return openF1Client.getWeather(raceSessionKey).stream()
                .max(Comparator.comparing(w -> w.airTemperature() != null ? w.airTemperature() : Float.MIN_VALUE)) // 예시: 기온이 가장 높은 기록
                .map(w -> {
                    Map<String, Object> weatherMap = new HashMap<>();
                    weatherMap.put("air_temperature", w.airTemperature());
                    weatherMap.put("track_temperature", w.trackTemperature());
                    weatherMap.put("rainfall", w.rainfall());
                    return weatherMap;
                })
                .orElse(Collections.emptyMap());
    }
}