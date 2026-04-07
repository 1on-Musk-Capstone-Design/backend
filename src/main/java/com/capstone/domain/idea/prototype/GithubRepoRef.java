package com.capstone.domain.idea.prototype;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code https://github.com/org/repo} 형태 URL에서 org·repo 추출.
 */
public final class GithubRepoRef {

  private static final Pattern PATTERN =
      Pattern.compile("^https?://github\\.com/([^/]+)/([^/]+?)(?:\\.git)?/?$", Pattern.CASE_INSENSITIVE);

  private GithubRepoRef() {}

  public static Optional<Record> parse(String githubRepoUrl) {
    if (githubRepoUrl == null || githubRepoUrl.isBlank()) {
      return Optional.empty();
    }
    Matcher m = PATTERN.matcher(githubRepoUrl.trim());
    if (!m.matches()) {
      return Optional.empty();
    }
    return Optional.of(new Record(m.group(1), m.group(2)));
  }

  public record Record(String org, String repo) {}
}
