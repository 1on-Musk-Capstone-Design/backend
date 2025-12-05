package com.capstone.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

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
    
    // /uploads/** 경로로 모든 업로드 파일 서빙 (thumbnails 포함)
    // upload-dir이 thumbnail-dir의 부모 디렉토리인 경우 하나로 처리 가능
    if (thumbnailPathStr.startsWith(uploadPathStr)) {
      // thumbnail-dir이 upload-dir 안에 있으면 하나의 경로로 처리
      registry.addResourceHandler("/uploads/**")
          .addResourceLocations("file:" + uploadPathStr + "/");
    } else {
      // 별도 디렉토리인 경우 각각 등록
      registry.addResourceHandler("/uploads/**")
          .addResourceLocations("file:" + uploadPathStr + "/");
      registry.addResourceHandler("/uploads/thumbnails/**")
          .addResourceLocations("file:" + thumbnailPathStr + "/");
    }
  }
}

