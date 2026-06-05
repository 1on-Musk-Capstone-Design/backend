package com.capstone.global.oauth;

import jakarta.validation.constraints.NotBlank;

public record AppleLoginRequest(
    @NotBlank(message = "Identity token은 필수입니다")
    String identityToken,
    String fullName
) {}