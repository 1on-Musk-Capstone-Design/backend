package com.capstone.global.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

  @Value("${app.file.upload-dir:./uploads}")
  private String uploadDir;

  @Value("${app.file.thumbnail-dir:./uploads/thumbnails}")
  private String thumbnailDir;

  @Value("${app.file.base-url:http://localhost:8080/api}")
  private String baseUrl;

  /**
   * 썸네일 이미지를 저장하고 URL을 반환합니다.
   *
   * @param file 업로드할 파일
   * @param workspaceId 워크스페이스 ID
   * @return 저장된 파일의 URL
   * @throws IOException 파일 저장 실패 시
   */
  public String saveThumbnail(MultipartFile file, Long workspaceId) throws IOException {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("파일이 비어있습니다.");
    }

    // 파일 확장자 확인
    String originalFilename = file.getOriginalFilename();
    String extension = "";
    if (originalFilename != null && originalFilename.contains(".")) {
      extension = originalFilename.substring(originalFilename.lastIndexOf("."));
    }
    
    // 이미지 파일만 허용
    if (!extension.matches("\\.(png|jpg|jpeg|gif|webp)$")) {
      throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
    }

    // 디렉토리 생성
    Path thumbnailPath = Paths.get(thumbnailDir).toAbsolutePath().normalize();
    Files.createDirectories(thumbnailPath);

    // 파일명 생성: workspace-{id}-{uuid}.{extension}
    String filename = String.format("workspace-%d-%s%s", workspaceId, UUID.randomUUID(), extension);
    Path targetPath = thumbnailPath.resolve(filename);

    // 파일 저장
    Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
    log.info("썸네일 저장 완료: {}", targetPath);

    // URL 생성: /api/uploads/thumbnails/{filename}
    String url = baseUrl + "/uploads/thumbnails/" + filename;
    return url;
  }

  /**
   * 기존 썸네일 파일을 삭제합니다.
   *
   * @param thumbnailUrl 삭제할 썸네일 URL
   */
  public void deleteThumbnail(String thumbnailUrl) {
    if (thumbnailUrl == null || thumbnailUrl.isEmpty()) {
      return;
    }

    try {
      // URL에서 파일명 추출
      String filename = thumbnailUrl.substring(thumbnailUrl.lastIndexOf("/") + 1);
      Path filePath = Paths.get(thumbnailDir).resolve(filename).toAbsolutePath().normalize();
      
      // 파일이 존재하면 삭제
      if (Files.exists(filePath)) {
        Files.delete(filePath);
        log.info("썸네일 삭제 완료: {}", filePath);
      }
    } catch (IOException e) {
      log.warn("썸네일 삭제 실패: {}", thumbnailUrl, e);
    }
  }
}

