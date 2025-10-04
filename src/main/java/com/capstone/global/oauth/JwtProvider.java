package com.capstone.global.oauth;

import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

  private final SecretKey secretKey;
  private static final long ACCESS_TOKEN_EXPIRE_TIME = 1000L * 60 * 60;
  private static final long REFRESH_TOKEN_EXPIRE_TIME = 1000L * 60 * 60 * 24 * 7;

  public JwtProvider(@Value("${spring.jwt.secret}") String secret) {
    this.secretKey = new SecretKeySpec(
        secret.getBytes(StandardCharsets.UTF_8),
        Jwts.SIG.HS256.key().build().getAlgorithm()
    );
  }

  public String createAccessToken(Long userId, String email) {
    return Jwts.builder()
        .subject(email)
        .claim("user_id", userId)
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRE_TIME))
        .signWith(secretKey)
        .compact();
  }

  public String createRefreshToken(Long userId, String email) {
    return Jwts.builder()
        .subject(email)
        .claim("user_id", userId)
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRE_TIME))
        .signWith(secretKey)
        .compact();
  }
}