package com.gridhub.gridhub.domain.f1data.controller;

import com.gridhub.gridhub.domain.f1data.dto.RaceCalendarDto;
import com.gridhub.gridhub.domain.f1data.service.F1DataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Year;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/f1-data")
public class F1DataController {

    private final F1DataService f1DataService;

    @GetMapping("/calendar")
    public ResponseEntity<List<RaceCalendarDto>> getRaceCalendar(
            @RequestParam(value = "year", defaultValue = "0") int year
    ) {
        // year 파라미터가 없거나 0이면 현재 연도로 설정
        int targetYear = (year == 0) ? Year.now().getValue() : year;

        List<RaceCalendarDto> calendar =
                f1DataService.getRaceCalendarByYear(targetYear);

        return ResponseEntity.ok(calendar);
    }
}
