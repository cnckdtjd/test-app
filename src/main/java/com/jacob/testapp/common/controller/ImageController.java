package com.jacob.testapp.common.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/img")
public class ImageController {

    private final ResourceLoader resourceLoader;
    
    // 이미지 압축 품질 (0.0 ~ 1.0)
    private static final float IMAGE_COMPRESSION_QUALITY = 0.7f;
    
    // 캐시 유효 기간 (초)
    private static final long CACHE_MAX_AGE_SECONDS = 604800; // 1주일
    
    public ImageController(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
    
    @GetMapping("/{filename:.+}")
    public ResponseEntity<byte[]> getImage(@PathVariable String filename) throws IOException {
        // 이미지 리소스 로드
        Resource resource = resourceLoader.getResource("classpath:static/img/" + filename);
        
        // 리소스가 존재하는지 확인
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        
        // 이미지 포맷 확인 (파일 확장자 추출)
        String fileExtension = getFileExtension(filename).toLowerCase();
        
        // 이미지 최적화 및 압축
        byte[] optimizedImageData;
        try (InputStream inputStream = resource.getInputStream()) {
            optimizedImageData = optimizeImage(inputStream, fileExtension);
        }
        
        // 응답 헤더 설정 (콘텐츠 타입 및 캐싱)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(getMediaType(fileExtension));
        
        // ETag 헤더 추가 (파일명 기반)
        String etag = "W/\"" + filename.hashCode() + "\"";
        headers.setETag(etag);
        
        // 응답 반환 (최적화된 이미지 + 캐싱 헤더)
        return ResponseEntity.ok()
                .headers(headers)
                .cacheControl(CacheControl.maxAge(CACHE_MAX_AGE_SECONDS, TimeUnit.SECONDS)
                        .cachePublic())
                .body(optimizedImageData);
    }
    
    /**
     * 이미지 최적화 및 압축
     */
    private byte[] optimizeImage(InputStream inputStream, String formatName) throws IOException {
        // 이미지 로드
        BufferedImage image = ImageIO.read(inputStream);
        if (image == null) {
            throw new IOException("이미지를 로드할 수 없습니다.");
        }
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        // JPG 이미지인 경우 압축 적용
        if (formatName.equals("jpg") || formatName.equals("jpeg")) {
            // 이미지 라이터 및 파라미터 설정
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(formatName);
            if (!writers.hasNext()) {
                throw new IOException("지원되지 않는 이미지 형식: " + formatName);
            }
            
            ImageWriter writer = writers.next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(IMAGE_COMPRESSION_QUALITY);
            
            // 이미지 압축 및 출력
            try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {
                writer.setOutput(imageOutputStream);
                writer.write(null, new IIOImage(image, null, null), param);
                writer.dispose();
            }
        } else {
            // 다른 형식의 이미지는 표준 방식으로 저장
            ImageIO.write(image, formatName, outputStream);
        }
        
        return outputStream.toByteArray();
    }
    
    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? "" : filename.substring(dotIndex + 1);
    }
    
    /**
     * 파일 확장자에 따른 MediaType 반환
     */
    private MediaType getMediaType(String extension) {
        switch (extension.toLowerCase()) {
            case "jpg":
            case "jpeg":
                return MediaType.IMAGE_JPEG;
            case "png":
                return MediaType.IMAGE_PNG;
            case "gif":
                return MediaType.IMAGE_GIF;
            default:
                return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
} 