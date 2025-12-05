package com.capstone.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  @Value("${app.file.upload-dir:./uploads}")
  private String uploadDir;

  @Value("${app.file.thumbnail-dir:./uploads/thumbnails}")
  private String thumbnailDir;

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // 업로드된 파일들을 정적 리소스로 서빙
    Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
    String uploadPathStr = uploadPath.toString().replace("\\", "/");
    
    // 썸네일 디렉토리 경로
    Path thumbnailPath = Paths.get(thumbnailDir).toAbsolutePath().normalize();
    String thumbnailPathStr = thumbnailPath.toString().replace("\\", "/");
    
    log.info("=== 정적 파일 서빙 설정 ===");
    log.info("upload-dir: {}", uploadPathStr);
    log.info("thumbnail-dir: {}", thumbnailPathStr);
    
    // /uploads/** 경로로 모든 업로드 파일 서빙 (thumbnails 포함)
    // upload-dir이 thumbnail-dir의 부모 디렉토리인 경우 하나로 처리 가능
    if (thumbnailPathStr.startsWith(uploadPathStr)) {
      // thumbnail-dir이 upload-dir 안에 있으면 하나의 경로로 처리
      log.info("썸네일 디렉토리가 업로드 디렉토리 안에 있음 - 단일 경로로 서빙");
      registry.addResourceHandler("/uploads/**")
          .addResourceLocations("file:" + uploadPathStr + "/");
      log.info("정적 리소스 매핑: /uploads/** -> file:{}/", uploadPathStr);
    } else {
      // 별도 디렉토리인 경우 각각 등록
      log.info("썸네일 디렉토리가 별도 위치 - 각각 등록");
      registry.addResourceHandler("/uploads/**")
          .addResourceLocations("file:" + uploadPathStr + "/");
      registry.addResourceHandler("/uploads/thumbnails/**")
          .addResourceLocations("file:" + thumbnailPathStr + "/");
      log.info("정적 리소스 매핑: /uploads/** -> file:{}/", uploadPathStr);
      log.info("정적 리소스 매핑: /uploads/thumbnails/** -> file:{}/", thumbnailPathStr);
    }
  }
}

