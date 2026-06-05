package com.capstone.global.oauth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtProvider {

  @Value("${spring.jwt.secret}")
  private String secretKey;

  private static final long ACCESS_TOKEN_EXPIRE_TIME = 1000L * 60 * 60 * 24;
  private static final long REFRESH_TOKEN_EXPIRE_TIME = 1000L * 60 * 60 * 24 * 7;

  public String createAccessToken(Long userId, String email) {
    return Jwts.builder()
        .subject(email)
        .claim("user_id", userId)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRE_TIME))
        .signWith(getSigningKey(), SignatureAlgorithm.HS256)
        .compact();
  }

  public String createRefreshToken(Long userId, String email) {
    return Jwts.builder()
        .subject(email)
        .claim("user_id", userId)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRE_TIME))
        .signWith(getSigningKey(), SignatureAlgorithm.HS256)
        .compact();
  }

  public Long getUserIdFromAccessToken(String token) {
    Claims claims = Jwts.parser()
        .setSigningKey(getSigningKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();

    Object userIdObj = claims.get("user_id");
    if (userIdObj == null) {
      throw new IllegalArgumentException("토큰에 user_id가 없습니다.");
    }
    
    // Integer 또는 Long 타입 모두 처리
    if (userIdObj instanceof Long) {
      return (Long) userIdObj;
    } else if (userIdObj instanceof Integer) {
      return ((Integer) userIdObj).longValue();
    } else if (userIdObj instanceof Number) {
      return ((Number) userIdObj).longValue();
    } else {
      // String으로 저장된 경우도 처리
      try {
        return Long.parseLong(userIdObj.toString());
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("토큰의 user_id가 유효한 숫자가 아닙니다: " + userIdObj);
      }
    }
  }

  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
  }
}