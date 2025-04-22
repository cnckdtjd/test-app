package com.jacob.testapp.admin.service;

import lombok.extern.slf4j.Slf4j;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 통계 관련 공통 유틸리티 클래스
 */
@Slf4j
public class StatisticsUtil {

    /**
     * 연-월 포맷터 (예: 2023-05)
     */
    public static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    
    /**
     * 월-일 포맷터 (예: 05-20)
     */
    public static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

    /**
     * 오늘 시작 시간
     */
    public static LocalDateTime getTodayStart() {
        return LocalDate.now().atStartOfDay();
    }

    /**
     * 오늘 종료 시간
     */
    public static LocalDateTime getTodayEnd() {
        return LocalDate.now().atTime(LocalTime.MAX);
    }

    /**
     * 이번 달 시작 시간
     */
    public static LocalDateTime getMonthStart() {
        return LocalDate.now().withDayOfMonth(1).atStartOfDay();
    }

    /**
     * 이번 달 종료 시간 (현재까지)
     */
    public static LocalDateTime getMonthEnd() {
        return LocalDate.now().atTime(LocalTime.MAX);
    }

    /**
     * 지정된 일수 이전 시간
     */
    public static LocalDateTime getDaysAgo(int days) {
        return LocalDateTime.now().minusDays(days);
    }

    /**
     * 통계 데이터 조회를 위한 안전한 처리 메서드
     * @param statsSupplier 통계 데이터를 제공하는 함수
     * @param errorMessage 오류 발생시 로그 메시지
     * @return 통계 데이터 또는 오류 정보가 포함된 맵
     */
    public static Map<String, Object> safelyGetStatistics(
            Supplier<Map<String, Object>> statsSupplier, 
            String errorMessage) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            return statsSupplier.get();
        } catch (Exception e) {
            log.error(errorMessage, e);
            stats.put("error", "통계 정보를 로드하는 중 오류가 발생했습니다");
            return stats;
        }
    }

    /**
     * 지정된 키/값을 기존 맵에 추가
     */
    public static Map<String, Object> putAll(Map<String, Object> target, Map<String, Object> source) {
        target.putAll(source);
        return target;
    }

    /**
     * 맵에 값 추가 헬퍼 메서드
     */
    public static void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
} 