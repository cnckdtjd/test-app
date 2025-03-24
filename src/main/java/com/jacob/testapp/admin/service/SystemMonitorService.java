package com.jacob.testapp.admin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * 시스템 자원 사용량을 모니터링하는 서비스
 */
@Service
public class SystemMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(SystemMonitorService.class);
    private final DecimalFormat decimalFormat = new DecimalFormat("#0.00");
    
    /**
     * 시스템 리소스 정보를 수집하여 반환합니다.
     * CPU 사용률, 메모리 사용량, JVM 힙 메모리 정보 등을 포함합니다.
     * 
     * @return 시스템 리소스 정보를 담은 Map
     */
    public Map<String, Object> getSystemResources() {
        Map<String, Object> resources = new HashMap<>();
        
        try {
            // CPU 사용률 정보
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            double cpuLoad = getCpuLoad(osBean);
            resources.put("cpuUsage", decimalFormat.format(cpuLoad * 100));
            resources.put("cpuUsageRaw", cpuLoad * 100); // 차트용 원시 데이터
            resources.put("availableProcessors", osBean.getAvailableProcessors());
            
            // JVM 메모리 정보
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long heapMemoryUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMemoryMax = memoryBean.getHeapMemoryUsage().getMax();
            long nonHeapMemoryUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
            
            double heapUsagePercent = (double) heapMemoryUsed / heapMemoryMax * 100;
            resources.put("heapMemoryUsed", formatSize(heapMemoryUsed));
            resources.put("heapMemoryMax", formatSize(heapMemoryMax));
            resources.put("heapMemoryUsedBytes", heapMemoryUsed);
            resources.put("heapMemoryMaxBytes", heapMemoryMax);
            resources.put("heapUsagePercent", decimalFormat.format(heapUsagePercent));
            resources.put("heapUsageRaw", heapUsagePercent); // 차트용 원시 데이터
            resources.put("nonHeapMemoryUsed", formatSize(nonHeapMemoryUsed));
            
            // 시스템 메모리 정보 (가능한 경우)
            try {
                com.sun.management.OperatingSystemMXBean sunOsBean = 
                    (com.sun.management.OperatingSystemMXBean) osBean;
                
                // macOS에서는 이 방식이 더 정확합니다
                long totalPhysicalMemory = sunOsBean.getTotalMemorySize();
                // 실제 사용 중인 메모리 측정 (가상 메모리 대신)
                long freePhysicalMemory = sunOsBean.getFreeMemorySize();
                long usedPhysicalMemory = totalPhysicalMemory - freePhysicalMemory;
                
                // 가상 메모리가 아닌 실제 물리 메모리 사용률 계산
                double memoryUsagePercent = (double) usedPhysicalMemory / totalPhysicalMemory * 100;
                memoryUsagePercent = Math.min(memoryUsagePercent, 100.0);
                
                resources.put("totalMemory", formatSize(totalPhysicalMemory));
                resources.put("usedMemory", formatSize(usedPhysicalMemory));
                resources.put("freeMemory", formatSize(freePhysicalMemory));
                resources.put("memoryUsagePercent", decimalFormat.format(memoryUsagePercent));
                resources.put("memoryUsageRaw", memoryUsagePercent);
            } catch (ClassCastException e) {
                logger.warn("시스템 메모리 정보를 가져오는데 실패했습니다. 제한된 모니터링 정보만 표시됩니다.", e);
                resources.put("memoryInfoError", "시스템 메모리 정보를 가져오는데 실패했습니다.");
                resources.put("memoryUsageRaw", 0.0); // 기본값 설정
            }
            
            // 가비지 컬렉션 정보
            resources.put("gcInfo", getGarbageCollectionInfo());
            
            // 런타임 정보
            resources.put("jvmStartTime", 
                formatUptime(ManagementFactory.getRuntimeMXBean().getStartTime()));
            resources.put("jvmUptime", 
                formatDuration(ManagementFactory.getRuntimeMXBean().getUptime()));
            
        } catch (Exception e) {
            logger.error("시스템 리소스 정보를 수집하는 중 오류가 발생했습니다.", e);
            resources.put("error", "시스템 리소스 정보를 수집하는 중 오류가 발생했습니다.");
            // 기본값 설정
            resources.put("cpuUsage", "0.00");
            resources.put("cpuUsageRaw", 0.0);
            resources.put("heapMemoryUsed", "0 MB");
            resources.put("heapMemoryMax", "0 MB");
            resources.put("heapUsagePercent", "0.00");
            resources.put("heapUsageRaw", 0.0);
            resources.put("totalMemory", "0 MB");
            resources.put("usedMemory", "0 MB");
            resources.put("freeMemory", "0 MB");
            resources.put("memoryUsagePercent", "0.00");
            resources.put("memoryUsageRaw", 0.0);
        }
        
        return resources;
    }
    
    /**
     * CPU 사용률을 계산합니다.
     * 플랫폼 제한으로 인해 정확한 값을 얻지 못할 수 있습니다.
     * 
     * @param osBean OperatingSystemMXBean 인스턴스
     * @return CPU 사용률 (0.0 ~ 1.0)
     */
    private double getCpuLoad(OperatingSystemMXBean osBean) {
        double load = -1.0;
        
        try {
            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
                load = sunOsBean.getCpuLoad();
                
                // -1.0은 첫 번째 호출 시 값을 사용할 수 없음을 나타냄
                if (load < 0) {
                    // 첫 번째 호출은 종종 -1.0을 반환하므로 다시 시도
                    try {
                        Thread.sleep(500);
                        load = sunOsBean.getCpuLoad();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            
            if (load < 0) {
                // 플랫폼이나 Java 버전이 정확한 CPU 사용률을 지원하지 않는 경우
                load = osBean.getSystemLoadAverage() / osBean.getAvailableProcessors();
                // 여전히 값이 없다면 기본값 사용
                if (load < 0) {
                    load = 0.0;
                }
            }
        } catch (Exception e) {
            logger.warn("CPU 사용률을 가져오는데 실패했습니다.", e);
            load = 0.0;
        }
        
        return Math.min(Math.max(0.0, load), 1.0); // 0.0 ~ 1.0 범위로 제한
    }
    
    /**
     * 가비지 컬렉션 정보를 수집합니다.
     * 
     * @return 가비지 컬렉션 정보를 담은 Map
     */
    private Map<String, Object> getGarbageCollectionInfo() {
        Map<String, Object> gcInfo = new HashMap<>();
        
        try {
            long totalGcCount = 0;
            long totalGcTime = 0;
            
            for (java.lang.management.GarbageCollectorMXBean gc : 
                    ManagementFactory.getGarbageCollectorMXBeans()) {
                long count = gc.getCollectionCount();
                long time = gc.getCollectionTime();
                
                if (count >= 0) {
                    totalGcCount += count;
                }
                
                if (time >= 0) {
                    totalGcTime += time;
                }
                
                gcInfo.put(gc.getName() + ".count", count);
                gcInfo.put(gc.getName() + ".time", formatDuration(time));
            }
            
            gcInfo.put("totalGcCount", totalGcCount);
            gcInfo.put("totalGcTime", formatDuration(totalGcTime));
            
        } catch (Exception e) {
            logger.warn("가비지 컬렉션 정보를 가져오는데 실패했습니다.", e);
            gcInfo.put("error", "가비지 컬렉션 정보를 가져오는데 실패했습니다.");
        }
        
        return gcInfo;
    }
    
    /**
     * 바이트 단위의 크기를 사람이 읽기 쉬운 형식으로 변환합니다.
     * 
     * @param bytes 바이트 단위의 크기
     * @return 형식화된 크기 문자열 (예: "4.2 MB")
     */
    private String formatSize(long bytes) {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return df.format(bytes / 1024.0) + " KB";
        } else if (bytes < 1024 * 1024 * 1024) {
            return df.format(bytes / (1024.0 * 1024.0)) + " MB";
        } else {
            return df.format(bytes / (1024.0 * 1024.0 * 1024.0)) + " GB";
        }
    }
    
    /**
     * 타임스탬프를 날짜/시간 문자열로 변환합니다.
     * 
     * @param timestamp 타임스탬프 (밀리초)
     * @return 형식화된 날짜/시간 문자열
     */
    private String formatUptime(long timestamp) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(timestamp));
    }
    
    /**
     * 밀리초 단위의 시간을 사람이 읽기 쉬운 형식으로 변환합니다.
     * 
     * @param millis 밀리초 단위의 시간
     * @return 형식화된 시간 문자열 (예: "3h 42m 15s")
     */
    private String formatDuration(long millis) {
        if (millis < 0) {
            return "N/A";
        }
        
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        seconds %= 60;
        minutes %= 60;
        hours %= 24;
        
        StringBuilder sb = new StringBuilder();
        
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0 || days > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0 || days > 0) {
            sb.append(minutes).append("m ");
        }
        sb.append(seconds).append("s");
        
        return sb.toString();
    }
} 