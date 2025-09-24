package com.gridhub.gridhub.domain.f1data.entity;

import com.gridhub.gridhub.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * F1 레이스 '일정' 정보를 담는 엔티티.
 * Open F1 API의 Session 데이터를 기반으로 생성되며, 결과 데이터는 RaceResult 엔티티와 1:1 관계를 통해 관리.
 */
@Entity
@Table(name = "race")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Race extends BaseTimeEntity {

    /**
     * 세션 고유 키 (Open F1 API의 session_key)
     */
    @Id
    @Column(name = "session_key")
    private Long id;

    /**
     * 세션 이름 (e.g., "Race", "Qualifying", "Practice 1")
     */
    @Column(nullable = false)
    private String sessionName;

    /**
     * 세션 시작 시간 (UTC)
     */
    @Column(nullable = false)
    private ZonedDateTime dateStart;

    /**
     * 세션 종료 시간 (UTC)
     */
    @Column(nullable = false)
    private ZonedDateTime dateEnd;

    /**
     * 그랑프리(Meeting)의 고유 키
     */
    @Column(nullable = false)
    private Long meetingKey;

    /**
     * 그랑프리(Meeting)의 이름 (e.g., "Monaco Grand Prix")
     */
    @Column(nullable = false)
    private String meetingName;

    /**
     * 개최 국가
     */
    @Column(nullable = false)
    private String countryName;

    /**
     * 서킷 약칭
     */
    @Column(nullable = false)
    private String circuitShortName;

    /**
     * 시즌 연도
     */
    @Column(name = "race_year", nullable = false)
    private Integer year;

    /**
     * 이 Race(세션)에 대한 결과 정보. 1:1 양방향 관계.
     * cascade = CascadeType.ALL: Race가 저장/삭제될 때 RaceResult도 함께 처리.
     * orphanRemoval = true: Race와의 연관관계가 끊어지면 RaceResult는 자동으로 삭제.
     */
    @OneToOne(mappedBy = "race", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private RaceResult raceResult;

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

    // == 연관관계 편의 메서드 == //
    public void setRaceResult(RaceResult raceResult) {
        this.raceResult = raceResult;
        if (raceResult != null && raceResult.getRace() != this) {
            raceResult.setRace(this); // RaceResult에도 Race 객체를 설정 (양방향)
        }
    }
}