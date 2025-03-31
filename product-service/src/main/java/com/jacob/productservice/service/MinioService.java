package com.jacob.productservice.service;

import io.minio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.util.UUID;

@Service
public class MinioService {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.endpoint}")
    private String minioEndpoint;

    @PostConstruct
    public void init() {
        try {
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("MinIO 버킷 초기화 실패", e);
        }
    }

    public String uploadImage(MultipartFile file) {
        try {
            // 파일 이름 생성 (UUID 사용)
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            
            // MinIO에 파일 업로드
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(fileName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );
            
            // 이미지 URL 반환
            return String.format("%s/%s/%s", minioEndpoint, bucket, fileName);
        } catch (Exception e) {
            throw new RuntimeException("파일 업로드 실패", e);
        }
    }
    
    public String getImageUrl(String objectName) {
        return String.format("%s/%s/%s", minioEndpoint, bucket, objectName);
    }
} 