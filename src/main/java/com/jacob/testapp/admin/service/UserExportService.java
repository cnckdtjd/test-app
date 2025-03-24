package com.jacob.testapp.admin.service;

import com.jacob.testapp.user.entity.User;
import com.jacob.testapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 사용자 정보를 CSV 파일로 내보내는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserExportService {

    private final UserRepository userRepository;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 모든 사용자의 기본 정보를 CSV 형식으로 내보내기
     */
    public byte[] exportUsersToCsv() {
        log.info("사용자 정보 CSV 내보내기 시작");
        
        List<User> users = userRepository.findAll();
        log.info("내보낼 사용자 수: {}", users.size());
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            // BOM 마커 추가 (Excel에서 UTF-8 인식을 위함)
            writer.write("\uFEFF");
            
            // CSV 헤더
            writer.println("ID,Username,Email,Name,Role,Enabled,Account Locked,Created At");
            
            // 사용자 데이터 작성
            for (User user : users) {
                StringBuilder line = new StringBuilder();
                line.append(user.getId()).append(",");
                line.append(escapeSpecialCharacters(user.getUsername())).append(",");
                line.append(escapeSpecialCharacters(user.getEmail())).append(",");
                line.append(escapeSpecialCharacters(user.getName())).append(",");
                line.append(user.getRole()).append(",");
                line.append(user.isEnabled()).append(",");
                line.append(user.isAccountLocked()).append(",");
                
                if (user.getCreatedAt() != null) {
                    line.append(user.getCreatedAt().format(DATE_FORMATTER));
                }
                
                writer.println(line);
            }
            
            writer.flush();
            log.info("사용자 {} 명의 정보를 CSV로 내보내기 완료", users.size());
            return out.toByteArray();
        } catch (Exception e) {
            log.error("사용자 정보 CSV 내보내기 중 오류 발생", e);
            throw new RuntimeException("사용자 데이터 내보내기 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 사용자 인증 정보(ID/PW)를 CSV 형식으로 내보내기
     * 각 행은 username,password 형식으로 저장됩니다.
     * 
     * 참고: 실제 운영 환경에서 이 기능은 보안 위험을 초래할 수 있습니다.
     */
    public byte[] exportUserCredentialsToCsv() {
        log.info("사용자 인증 정보 CSV 내보내기 시작");
        
        List<User> users = userRepository.findAll();
        log.info("내보낼 사용자 인증정보 수: {}", users.size());
        
        if (users.isEmpty()) {
            log.warn("내보낼 사용자가 없습니다!");
        }
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        try (OutputStreamWriter osw = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             PrintWriter writer = new PrintWriter(osw, true)) {
            
            // BOM 마커 추가 (Excel에서 UTF-8 인식을 위함)
            osw.write("\uFEFF");
            
            // 헤더 추가
            writer.println("Username,Password");
            
            // 각 사용자에 대해 username,password 형식으로 작성
            for (User user : users) {
                // 사용자 ID와 비밀번호 확인
                if (user.getUsername() == null || user.getUsername().isEmpty()) {
                    log.warn("사용자 ID {}의 username이 비어 있습니다", user.getId());
                    continue;
                }
                
                // CSV 포맷에 맞게 사용자명 이스케이프 처리
                String username = escapeSpecialCharacters(user.getUsername());
                String password = "password" + user.getId();
                
                writer.println(username + "," + password);
                
            }
            
            writer.flush();
            osw.flush();
            
            byte[] result = out.toByteArray();
            log.info("사용자 인증정보 CSV 내보내기 완료: {}바이트", result.length);
            
            return result;
        } catch (Exception e) {
            log.error("사용자 인증 정보 CSV 내보내기 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("사용자 인증 데이터 내보내기 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * CSV에서 특수 문자 처리
     */
    private String escapeSpecialCharacters(String data) {
        if (data == null) {
            return "";
        }
        
        String escaped = data.replace("\"", "\"\"");
        
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            escaped = "\"" + escaped + "\"";
        }
        
        return escaped;
    }
} 