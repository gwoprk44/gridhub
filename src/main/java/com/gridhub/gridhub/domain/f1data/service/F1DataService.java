package com.gridhub.gridhub.domain.f1data.service;

import com.gridhub.gridhub.domain.f1data.dto.DriverInfoResponse;
import com.gridhub.gridhub.domain.f1data.dto.RaceCalendarDto;
import com.gridhub.gridhub.domain.f1data.dto.RaceDetailResponse;
import com.gridhub.gridhub.domain.f1data.dto.TeamInfoResponse;
import com.gridhub.gridhub.domain.f1data.entity.Race;
import com.gridhub.gridhub.domain.f1data.entity.RaceResult;
import com.gridhub.gridhub.domain.f1data.exception.RaceNotFoundException;
import com.gridhub.gridhub.domain.f1data.exception.RaceResultNotFoundException;
import com.gridhub.gridhub.domain.f1data.repository.DriverRepository;
import com.gridhub.gridhub.domain.f1data.repository.RaceRepository;
import com.gridhub.gridhub.domain.f1data.repository.RaceResultRepository;
import com.gridhub.gridhub.domain.f1data.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class F1DataService {

    private final RaceRepository raceRepository;
    private final RaceResultRepository raceResultRepository;
    private final DriverRepository driverRepository;
    private final TeamRepository teamRepository;

    @Cacheable(value = "raceCalendar", key = "#year")
    @Transactional(readOnly = true)
    public List<RaceCalendarDto> getRaceCalendarByYear(int year) {
        log.info("--- Caching Disabled: Fetching race calendar for year {} from DB ---", year); // 캐시 미적용 시 로그
        // 1. 해당연도의 모든 레이스 데이터를 DB에서 조회
        List<Race> allRacesForYear =
                raceRepository.findByYearOrderByDateStartAsc(year);

        // 2. 조회된 데이터를 meetingKey를 기준으로 그룹화
        Map<Long, List<Race>> racesGroupedByMeeting = allRacesForYear.stream()
                .collect(Collectors.groupingBy(Race::getMeetingKey));

        // 3. 그룹화한 데이터를 레이스캘린더 dto로 변환
        return racesGroupedByMeeting.values().stream()
                .map(RaceCalendarDto::from)
                .sorted(Comparator.comparing(dto -> dto.sessions().get(0).dateStart()))
                .collect(Collectors.toList());
    }

    @Cacheable(value = "raceDetail", key = "#raceId")
    @Transactional(readOnly = true)
    public RaceDetailResponse getRaceDetail(Long raceId) {
        log.info("--- Caching Disabled: Fetching race detail for race #{} from DB ---", raceId); // 캐시 미적용 시 로그
        // 1. raceId로 레이스 엔티티 조회(존재하지 않으면 예외 던짐)
        Race race = raceRepository.findById(raceId)
                .orElseThrow(RaceNotFoundException::new);

        // 2. 조회된 레이스와 연관된 raceResult 조회(존재하지 않으면 예외 던짐)
        RaceResult result = raceResultRepository.findByRaceWithDetails(race)
                .orElseThrow(RaceResultNotFoundException::new);

        // 3. 두 엔티티를 조합하여 최종 응답 dto 생성
        return RaceDetailResponse.of(race, result);
    }

    @Cacheable(value = "drivers")
    @Transactional(readOnly = true)
    public List<DriverInfoResponse> getAllDrivers() {
        log.info("--- Caching Disabled: Fetching drivers from DB ---"); // 캐시가 동작 안 할 때만 로그 출력
        return driverRepository.findAll().stream()
                .map(DriverInfoResponse::from)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "teams")
    @Transactional(readOnly = true)
    public List<TeamInfoResponse> getAllTeams() {
        log.info("--- Caching Disabled: Fetching teams from DB ---"); // 캐시가 동작 안 할 때만 로그 출력
        return teamRepository.findAllWithDrivers().stream()
                .map(TeamInfoResponse::from)
                .collect(Collectors.toList());
    }
}
