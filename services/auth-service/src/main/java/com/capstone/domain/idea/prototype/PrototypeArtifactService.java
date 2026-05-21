package com.capstone.domain.idea.prototype;

import com.capstone.domain.idea.prototype.dto.PrototypeSourceFileResponse;
import com.capstone.global.exception.CustomException;
import com.capstone.global.exception.ErrorCode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * PRD/프로토타입 AI 생성 물을 서버 로컬 디스크에 저장하고, API로만 조회하도록 합니다. {@code
 * /uploads}에 두지 않아 정적 공개 URL로 유출되지 않습니다.
 */
@Slf4j
@Service
public class PrototypeArtifactService {

  @Value("${app.prototype.artifact-base-dir:./data/prototype-artifacts}")
  private String artifactBaseDir;

  public void writeForJob(long jobId, Map<String, String> files) {
    if (files == null || files.isEmpty()) {
      return;
    }
    Path base = jobRoot(jobId);
    try {
      if (Files.exists(base)) {
        try (Stream<Path> walk = Files.walk(base)) {
          List<Path> paths =
              walk.sorted(Comparator.reverseOrder()).collect(Collectors.toList());
          for (Path p : paths) {
            if (!p.equals(base)) {
              Files.deleteIfExists(p);
            }
          }
        }
      }
      Files.createDirectories(base);
      for (Map.Entry<String, String> e : files.entrySet()) {
        String rel = normalizeRelativePath(e.getKey());
        Path target = base.resolve(rel).normalize();
        if (!target.startsWith(base)) {
          log.warn("prototype artifact path traversal blocked: {}", e.getKey());
          continue;
        }
        Files.createDirectories(target.getParent());
        Files.writeString(target, e.getValue() != null ? e.getValue() : "", StandardCharsets.UTF_8);
      }
      log.info("prototype artifacts written jobId={} fileCount={} base={}", jobId, files.size(), base);
    } catch (IOException ex) {
      log.error("prototype artifact write failed jobId={}", jobId, ex);
      throw new CustomException(ErrorCode.PROTOTYPE_ARTIFACT_IO_FAILED);
    }
  }

  public List<PrototypeSourceFileResponse> listForJob(long jobId) {
    Path base = jobRoot(jobId);
    if (!Files.isDirectory(base)) {
      throw new CustomException(ErrorCode.NOT_FOUND_PROTOTYPE_ARTIFACT);
    }
    try {
      List<PrototypeSourceFileResponse> out = new ArrayList<>();
      Files.walkFileTree(
          base,
          new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
              String rel = base.relativize(file).toString().replace('\\', '/');
              String content = Files.readString(file, StandardCharsets.UTF_8);
              out.add(PrototypeSourceFileResponse.builder().path(rel).content(content).build());
              return FileVisitResult.CONTINUE;
            }
          });
      out.sort(Comparator.comparing(PrototypeSourceFileResponse::getPath));
      return out;
    } catch (IOException e) {
      log.error("prototype artifact list failed jobId={}", jobId, e);
      throw new CustomException(ErrorCode.PROTOTYPE_ARTIFACT_IO_FAILED);
    }
  }

  public boolean hasArtifacts(long jobId) {
    return Files.isDirectory(jobRoot(jobId));
  }

  public String readFile(long jobId, String relativePath) {
    String rel = normalizeRelativePath(relativePath);
    Path base = jobRoot(jobId);
    Path target = base.resolve(rel).normalize();
    if (!target.startsWith(base) || !Files.exists(target) || !Files.isRegularFile(target)) {
      throw new CustomException(ErrorCode.NOT_FOUND_PROTOTYPE_ARTIFACT);
    }
    try {
      return Files.readString(target, StandardCharsets.UTF_8);
    } catch (IOException e) {
      log.error("prototype artifact read failed jobId={} path={}", jobId, rel, e);
      throw new CustomException(ErrorCode.PROTOTYPE_ARTIFACT_IO_FAILED);
    }
  }

  private Path jobRoot(long jobId) {
    return Path.of(artifactBaseDir).toAbsolutePath().normalize().resolve(String.valueOf(jobId));
  }

  private static String normalizeRelativePath(String raw) {
    if (raw == null || raw.isBlank()) {
      return "unnamed.txt";
    }
    String s = raw.replace('\\', '/').trim();
    while (s.startsWith("/")) {
      s = s.substring(1);
    }
    Set<String> bad = Set.of("..", ".");
    String out =
        Stream.of(s.split("/"))
            .filter(segment -> !segment.isBlank() && !bad.contains(segment))
            .collect(Collectors.joining("/"));
    return out.isEmpty() ? "unnamed.txt" : out;
  }
}
