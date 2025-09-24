// OpenF1Client.java (최종 리팩토링 버전)
package com.gridhub.gridhub.infra.external;

import com.gridhub.gridhub.domain.f1data.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenF1Client {

    private final WebClient openF1WebClient;

    // ================== 캘린더 및 기본 정보 조회 ==================

    /**
     * 특정 연도의 모든 그랑프리(Meeting) 정보를 조회.
     */
    public List<MeetingResponse> getMeetingsByYear(int year) {
        return performRequest("/meetings", "year", year, MeetingResponse.class)
                .collectList()
                .block();
    }

    /**
     * 특정 그랑프리(Meeting)에 포함된 모든 세션(연습, 퀄리, 레이스 등) 정보를 조회.
     */
    public List<SessionResponse> getSessionsByMeeting(long meetingKey) {
        return performRequest("/sessions", "meeting_key", meetingKey, SessionResponse.class)
                .collectList()
                .block();
    }

    /**
     * 특정 세션의 드라이버 라인업을 조회. 팀 색상 정보가 포함.
     */
    public List<DriverResponse> getDriversBySession(long sessionKey) {
        return performRequest("/drivers", "session_key", sessionKey, DriverResponse.class)
                .collectList()
                .block();
    }

    // ================== 레이스 결과 정보 조회 ==================

    /**
     * 특정 레이스 세션의 최종 순위(Position) 정보.
     *
     */
    public List<PositionResponse> getRaceResult(long raceSessionKey) {
        List<PositionResponse> allPositions = performRequest("/position", "session_key", raceSessionKey, PositionResponse.class)
                .collectList()
                .block();

        if (allPositions == null || allPositions.isEmpty()) {
            return Collections.emptyList();
        }

        // 가장 마지막 시간(date)의 기록을 드라이버별로 필터링
        // TODO: 경기 종료후 페널티 여부도 고려 이후 코드 리팩토링
        return allPositions.stream()
                .sorted(Comparator.comparing(PositionResponse::date).reversed())
                .map(PositionResponse::driverNumber)
                .distinct()
                .map(driverNum -> allPositions.stream()
                        .filter(p -> p.driverNumber().equals(driverNum))
                        .max(Comparator.comparing(PositionResponse::date))
                        .orElse(null))
                .toList();
    }

    /**
     * 특정 레이스 세션의 퀄리파잉 결과(그리드 순서).
     *
     */
    public List<PositionResponse> getQualifyingResult(long qualifyingSessionKey) {
        // 퀄리파잉 최종 결과는 position 정보의 마지막 기록과 동일.
        return getRaceResult(qualifyingSessionKey);
    }

    /**
     * 특정 레이스 세션의 주요 이벤트(세이프티카, 레드 플래그 등)를 조회.
     *
     */
    public List<RaceControlResponse> getRaceControlMessages(long raceSessionKey) {
        return performRequest("/race_control", "session_key", raceSessionKey, RaceControlResponse.class)
                .collectList()
                .block();
    }

    /**
     * 특정 레이스 세션의 날씨 정보를 조회.
     *
     */
    public List<WeatherResponse> getWeather(long raceSessionKey) {
        return performRequest("/weather", "session_key", raceSessionKey, WeatherResponse.class)
                .collectList()
                .block();
    }

    /**
     * WebClient 요청을 수행하는 중복 로직 추출
     */
    private <T> Flux<T> performRequest(String path, String queryParamName, Object queryParamValue, Class<T> responseType) {
        return openF1WebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(path)
                        .queryParam(queryParamName, queryParamValue)
                        .build())
                .retrieve()
                .bodyToFlux(responseType)
                .doOnError(e -> log.error("Failed to fetch data from OpenF1 API [path: {}, param: {}]", path, queryParamValue, e));
    }

    //TODO: 필요에 따라 추가적인 메서드 구현 예정
}