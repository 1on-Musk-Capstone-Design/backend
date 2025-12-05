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

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // 업로드된 파일들을 정적 리소스로 서빙
    Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
    String uploadPathStr = uploadPath.toString().replace("\\", "/");
    
    // /uploads/** 경로로 모든 업로드 파일 서빙 (thumbnails 포함)
    registry.addResourceHandler("/uploads/**")
        .addResourceLocations("file:" + uploadPathStr + "/");
  }
}

