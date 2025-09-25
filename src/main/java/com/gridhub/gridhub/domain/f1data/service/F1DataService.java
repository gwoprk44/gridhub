package com.gridhub.gridhub.domain.f1data.service;

import com.gridhub.gridhub.domain.f1data.dto.RaceCalendarDto;
import com.gridhub.gridhub.domain.f1data.entity.Race;
import com.gridhub.gridhub.domain.f1data.repository.RaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class F1DataService {

    private final RaceRepository raceRepository;

    @Transactional(readOnly = true)
    public List<RaceCalendarDto> getRaceCalendarByYear(int year) {
        // 1. 해당연도의 모든 레이스 데이터를 DB에서 조회
        List<Race> allRacesForYear =
                raceRepository.findByYearOrderByDateStartAsc(year);

        // 2. 조회된 데이터를 meetingKey를 기준으로 그룹화
        Map<Long, List<Race>> racesGroupedByMeeting = allRacesForYear.stream()
                .collect(Collectors.groupingBy(Race::getMeetingKey));

        // 3. 그룹화한 데이터를 레이스캘린더 dto로 변환
        return racesGroupedByMeeting.values().stream()
                .map(RaceCalendarDto::from)
                .collect(Collectors.toList());
    }
}
