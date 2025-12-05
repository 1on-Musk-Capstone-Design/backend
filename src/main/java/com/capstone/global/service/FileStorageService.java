package com.capstone.global.service;

import com.capstone.domain.idea.Idea;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
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

  /**
   * 워크스페이스의 실제 내용(아이디어들)을 기반으로 썸네일을 생성합니다.
   * 워크스페이스 내의 아이디어들을 작게 보여주는 미리보기 이미지를 생성합니다.
   *
   * @param workspaceName 워크스페이스 이름
   * @param workspaceId 워크스페이스 ID
   * @param ideas 워크스페이스의 아이디어 목록
   * @return 생성된 썸네일 이미지의 URL
   * @throws IOException 이미지 생성 또는 저장 실패 시
   */
  public String generateWorkspaceContentThumbnail(String workspaceName, Long workspaceId, List<Idea> ideas) throws IOException {
    // 디렉토리 생성
    Path thumbnailPath = Paths.get(thumbnailDir).toAbsolutePath().normalize();
    Files.createDirectories(thumbnailPath);

    // 썸네일 크기 (프론트엔드 표시에 맞게 작게 조정)
    int thumbnailWidth = 280;
    int thumbnailHeight = 210;

    // 아이디어가 없으면 기본 썸네일 생성
    if (ideas == null || ideas.isEmpty()) {
      return generateDefaultThumbnail(workspaceName, workspaceId);
    }

    // 아이디어들의 위치와 크기를 기반으로 캔버스 범위 계산
    double minX = Double.MAX_VALUE;
    double minY = Double.MAX_VALUE;
    double maxX = Double.MIN_VALUE;
    double maxY = Double.MIN_VALUE;

    for (Idea idea : ideas) {
      if (idea.getPositionX() != null && idea.getPositionY() != null) {
        double x = idea.getPositionX();
        double y = idea.getPositionY();
        double width = idea.getPatchSizeX() != null ? idea.getPatchSizeX() : 200;
        double height = idea.getPatchSizeY() != null ? idea.getPatchSizeY() : 100;

        minX = Math.min(minX, x);
        minY = Math.min(minY, y);
        maxX = Math.max(maxX, x + width);
        maxY = Math.max(maxY, y + height);
      }
    }

    // 캔버스 범위가 없으면 기본 썸네일 생성
    if (minX == Double.MAX_VALUE || minY == Double.MAX_VALUE) {
      return generateDefaultThumbnail(workspaceName, workspaceId);
    }

    // 패딩 추가 (10%)
    double padding = Math.max(maxX - minX, maxY - minY) * 0.1;
    double canvasWidth = maxX - minX + padding * 2;
    double canvasHeight = maxY - minY + padding * 2;

    // 스케일 비율 계산 (썸네일 크기에 맞추기)
    double scaleX = thumbnailWidth / canvasWidth;
    double scaleY = thumbnailHeight / canvasHeight;
    double scale = Math.min(scaleX, scaleY); // 비율 유지

    // 실제 렌더링 크기 계산
    int renderWidth = (int) (canvasWidth * scale);
    int renderHeight = (int) (canvasHeight * scale);
    int offsetX = (thumbnailWidth - renderWidth) / 2;
    int offsetY = (thumbnailHeight - renderHeight) / 2;

    // 이미지 생성
    BufferedImage image = new BufferedImage(thumbnailWidth, thumbnailHeight, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2d = image.createGraphics();

    // 안티앨리어싱 활성화
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

    // 배경색 (밝은 회색)
    g2d.setColor(new Color(245, 245, 245));
    g2d.fillRect(0, 0, thumbnailWidth, thumbnailHeight);

    // 캔버스 영역 배경 (흰색)
    g2d.setColor(Color.WHITE);
    g2d.fillRect(offsetX, offsetY, renderWidth, renderHeight);

    // 아이디어들을 박스 형태로 그리기
    g2d.setFont(new Font("Arial", Font.PLAIN, Math.max(8, (int)(12 * scale))));
    FontMetrics fm = g2d.getFontMetrics();

    for (Idea idea : ideas) {
      if (idea.getPositionX() == null || idea.getPositionY() == null || idea.getContent() == null) {
        continue;
      }

      // 아이디어 위치와 크기를 썸네일 좌표로 변환
      double ideaX = idea.getPositionX() - minX + padding;
      double ideaY = idea.getPositionY() - minY + padding;
      double ideaWidth = idea.getPatchSizeX() != null ? idea.getPatchSizeX() : 200;
      double ideaHeight = idea.getPatchSizeY() != null ? idea.getPatchSizeY() : 100;

      int x = offsetX + (int) (ideaX * scale);
      int y = offsetY + (int) (ideaY * scale);
      int width = Math.max(20, (int) (ideaWidth * scale));
      int height = Math.max(20, (int) (ideaHeight * scale));

      // 박스 그리기 (연한 노란색 배경)
      g2d.setColor(new Color(255, 255, 200));
      g2d.fillRect(x, y, width, height);

      // 박스 테두리
      g2d.setColor(new Color(200, 200, 150));
      g2d.setStroke(new BasicStroke(Math.max(1, (int)(2 * scale))));
      g2d.drawRect(x, y, width, height);

      // 텍스트 내용 (줄바꿈 처리)
      g2d.setColor(Color.BLACK);
      String content = idea.getContent();
      if (content != null && !content.isEmpty()) {
        // 텍스트가 박스 안에 맞도록 줄바꿈
        String[] words = content.split("\\s+");
        int lineHeight = fm.getHeight();
        int currentY = y + lineHeight;
        int maxWidth = width - 4;
        int maxLines = Math.max(1, height / lineHeight - 1);

        StringBuilder line = new StringBuilder();
        int lineCount = 0;
        for (String word : words) {
          String testLine = line.length() > 0 ? line + " " + word : word;
          int textWidth = fm.stringWidth(testLine);
          if (textWidth > maxWidth && line.length() > 0) {
            // 현재 줄 그리기
            g2d.drawString(line.toString(), x + 2, currentY);
            line = new StringBuilder(word);
            currentY += lineHeight;
            lineCount++;
            if (lineCount >= maxLines) break;
          } else {
            line = new StringBuilder(testLine);
          }
        }
        // 마지막 줄 그리기
        if (line.length() > 0 && lineCount < maxLines) {
          g2d.drawString(line.toString(), x + 2, currentY);
        }
      }
    }

    g2d.dispose();

    // 파일 저장
    String filename = String.format("workspace-%d-content.png", workspaceId);
    Path targetPath = thumbnailPath.resolve(filename);
    ImageIO.write(image, "png", targetPath.toFile());
    
    // 파일이 실제로 생성되었는지 확인
    boolean fileExists = Files.exists(targetPath);
    log.info("워크스페이스 내용 썸네일 생성 완료: {} (아이디어 {}개, 파일 존재: {})", 
        targetPath, ideas.size(), fileExists);
    
    if (!fileExists) {
      log.error("썸네일 파일 생성 실패 - 경로: {}", targetPath);
      throw new IOException("썸네일 파일 생성 실패: " + targetPath);
    }

    // URL 생성
    String url = baseUrl + "/uploads/thumbnails/" + filename;
    log.info("생성된 썸네일 URL: {}", url);
    return url;
  }

  /**
   * 워크스페이스 이름을 기반으로 기본 썸네일을 생성합니다.
   * 워크스페이스 이름의 이니셜을 랜덤 색상 배경 위에 표시합니다.
   *
   * @param workspaceName 워크스페이스 이름
   * @param workspaceId 워크스페이스 ID
   * @return 생성된 썸네일 이미지의 URL
   * @throws IOException 이미지 생성 또는 저장 실패 시
   */
  public String generateDefaultThumbnail(String workspaceName, Long workspaceId) throws IOException {
    // 디렉토리 생성
    Path thumbnailPath = Paths.get(thumbnailDir).toAbsolutePath().normalize();
    Files.createDirectories(thumbnailPath);

    // 워크스페이스 이름에서 이니셜 추출 (최대 2글자)
    String initials = extractInitials(workspaceName);
    
    // 랜덤 색상 생성 (워크스페이스 ID를 시드로 사용하여 일관성 유지)
    Color backgroundColor = generateColorFromId(workspaceId);
    Color textColor = getContrastColor(backgroundColor);

    // 이미지 생성 (280x210 - 프론트엔드 표시에 맞게 작게 조정)
    int width = 280;
    int height = 210;
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2d = image.createGraphics();

    // 안티앨리어싱 활성화
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    // 배경 그리기
    g2d.setColor(backgroundColor);
    g2d.fillRect(0, 0, width, height);

    // 텍스트 그리기
    g2d.setColor(textColor);
    g2d.setFont(new Font("Arial", Font.BOLD, 120));
    FontMetrics fm = g2d.getFontMetrics();
    int textWidth = fm.stringWidth(initials);
    int textHeight = fm.getHeight();
    
    // 중앙 정렬
    int x = (width - textWidth) / 2;
    int y = (height + textHeight) / 2 - fm.getDescent();
    
    g2d.drawString(initials, x, y);
    g2d.dispose();

    // 파일 저장
    String filename = String.format("workspace-%d-default.png", workspaceId);
    Path targetPath = thumbnailPath.resolve(filename);
    ImageIO.write(image, "png", targetPath.toFile());
    log.info("기본 썸네일 생성 완료: {}", targetPath);

    // URL 생성
    String url = baseUrl + "/uploads/thumbnails/" + filename;
    return url;
  }

  /**
   * 워크스페이스 이름에서 이니셜을 추출합니다.
   * 단어가 하나면 첫 2글자, 여러 단어면 각 단어의 첫 글자 (최대 2글자).
   */
  private String extractInitials(String name) {
    if (name == null || name.trim().isEmpty()) {
      return "WS";
    }

    String trimmed = name.trim();
    String[] words = trimmed.split("\\s+");

    if (words.length == 1) {
      // 단어가 하나면 첫 2글자
      return trimmed.substring(0, Math.min(2, trimmed.length())).toUpperCase();
    } else {
      // 여러 단어면 각 단어의 첫 글자 (최대 2글자)
      StringBuilder initials = new StringBuilder();
      for (int i = 0; i < Math.min(2, words.length); i++) {
        if (!words[i].isEmpty()) {
          initials.append(words[i].charAt(0));
        }
      }
      return initials.toString().toUpperCase();
    }
  }

  /**
   * 워크스페이스 ID를 기반으로 일관된 색상을 생성합니다.
   */
  private Color generateColorFromId(Long workspaceId) {
    // ID를 해시하여 색상 생성
    int hash = workspaceId.hashCode();
    
    // 밝은 색상 범위 (200-255)에서 선택
    int r = 100 + Math.abs(hash % 155);
    int g = 100 + Math.abs((hash * 31) % 155);
    int b = 100 + Math.abs((hash * 61) % 155);
    
    return new Color(r, g, b);
  }

  /**
   * 배경색과 대비되는 텍스트 색상을 반환합니다.
   * 밝은 배경이면 어두운 글자, 어두운 배경이면 밝은 글자.
   */
  private Color getContrastColor(Color backgroundColor) {
    // 밝기 계산 (0-1)
    double brightness = (backgroundColor.getRed() * 0.299 + 
                         backgroundColor.getGreen() * 0.587 + 
                         backgroundColor.getBlue() * 0.114) / 255.0;
    
    // 밝으면 어두운 색, 어두우면 밝은 색
    return brightness > 0.5 ? Color.BLACK : Color.WHITE;
  }
}

