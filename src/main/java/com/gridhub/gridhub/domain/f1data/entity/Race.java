package com.gridhub.gridhub.domain.f1data.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "race")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Race {

    @Id
    @Column(name = "session_key") // API의 session_key를 PK로 사용
    private Long id;

    private String sessionName;
    private ZonedDateTime dateStart;
    private ZonedDateTime dateEnd;

    private Long meetingKey;
    private String meetingName;
    private String countryName;
    private String circuitShortName;
    private Integer year;

    @Builder
    public Race(Long id, String sessionName, ZonedDateTime dateStart, ZonedDateTime dateEnd, Long meetingKey, String meetingName, String countryName, String circuitShortName, Integer year) {
        this.id = id;
        this.sessionName = sessionName;
        this.dateStart = dateStart;
        this.dateEnd = dateEnd;
        this.meetingKey = meetingKey;
        this.meetingName = meetingName;
        this.countryName = countryName;
        this.circuitShortName = circuitShortName;
        this.year = year;
    }
}