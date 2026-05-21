package com.capstone.global.oauth;

import static com.capstone.global.exception.ErrorCode.INVALID_TOKEN;

import com.capstone.global.exception.CustomException;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AppleJwtValidator {

  @Value("${apple.bundle-id}")
  private String appleBundleId;

  @Value("${apple.public-key-url}")
  private String applePublicKeyUrl;

  public Map<String, Object> validateAndGetClaims(String identityToken) throws Exception {
    SignedJWT signedJWT = SignedJWT.parse(identityToken);

    if (!verifySignature(signedJWT)) {
      throw new CustomException(INVALID_TOKEN);
    }

    Map<String, Object> claims = signedJWT.getJWTClaimsSet().getClaims();

    Date exp = signedJWT.getJWTClaimsSet().getExpirationTime();
    if (exp == null || exp.before(new Date())) {
      throw new CustomException(INVALID_TOKEN);
    }

    if (!"https://appleid.apple.com".equals(claims.get("iss"))) {
      throw new IllegalArgumentException("잘못된 토큰 발행자입니다.");
    }

    validateAudience(claims.get("aud"));

    return claims;
  }

  private boolean verifySignature(SignedJWT signedJWT) throws Exception {
    URL applePublicKeysUrl = URI.create(applePublicKeyUrl).toURL();

    JWKSet jwkSet = JWKSet.load(applePublicKeysUrl);
    String kid = signedJWT.getHeader().getKeyID();
    RSAKey rsaKey = (RSAKey) jwkSet.getKeyByKeyId(kid);

    if (rsaKey == null) {
      return false;
    }

    RSASSAVerifier verifier = new RSASSAVerifier(rsaKey.toRSAPublicKey());
    return signedJWT.verify(verifier);
  }

  private void validateAudience(Object audClaim) {
    boolean isValid = false;
    if (audClaim instanceof String) {
      isValid = appleBundleId.equals(audClaim);
    } else if (audClaim instanceof List) {
      isValid = ((List<?>) audClaim).contains(appleBundleId);
    }

    if (!isValid) {
      log.error("Audience 검증 실패. 기대값: {}, 실제값: {}", appleBundleId, audClaim);
      throw new CustomException(INVALID_TOKEN);
    }
  }
}