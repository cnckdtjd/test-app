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
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
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
            
            log.info("사용자 {} 명의 정보를 CSV로 내보내기 완료", users.size());
            return out.toByteArray();
        } catch (Exception e) {
            log.error("사용자 정보 CSV 내보내기 중 오류 발생", e);
            throw new RuntimeException("사용자 데이터 내보내기 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 사용자 인증 정보(ID/PW)를 CSV 형식으로 내보내기
     */
    public byte[] exportUserCredentialsToCsv() {
        log.info("사용자 인증 정보 CSV 내보내기 시작");
        
        List<User> users = userRepository.findAll();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            // CSV 헤더
            writer.println("ID,Username,Password,Email,Role");
            
            // 사용자 데이터 작성 (비밀번호는 해시 형태로만 제공)
            for (User user : users) {
                StringBuilder line = new StringBuilder();
                line.append(user.getId()).append(",");
                line.append(escapeSpecialCharacters(user.getUsername())).append(",");
                line.append("********").append(","); // 보안상 실제 패스워드는 제외하고 마스킹
                line.append(escapeSpecialCharacters(user.getEmail())).append(",");
                line.append(user.getRole());
                
                writer.println(line);
            }
            
            log.info("사용자 {} 명의 인증 정보를 CSV로 내보내기 완료", users.size());
            return out.toByteArray();
        } catch (Exception e) {
            log.error("사용자 인증 정보 CSV 내보내기 중 오류 발생", e);
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