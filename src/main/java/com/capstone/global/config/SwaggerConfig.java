package com.capstone.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

  @Bean
  public OpenAPI openAPI() {
    // JWT 보안 스킴 정의
    String jwtSchemeName = "Bearer Authentication";
    SecurityRequirement securityRequirement = new SecurityRequirement()
        .addList(jwtSchemeName);

    Components components = new Components()
        .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
            .name(jwtSchemeName)
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("JWT 토큰을 입력하세요 (Bearer 제외)")
        );

    return new OpenAPI()
        .info(apiInfo())
        .servers(List.of(
            new Server().url("http://localhost:8080").description("로컬 개발 서버"),
            new Server().url("https://api.yourdomain.com").description("프로덕션 서버")
        ))
        .addSecurityItem(securityRequirement)
        .components(components);
  }

  private Info apiInfo() {
    return new Info()
        .title("Capstone API Documentation")
        .description("실시간 협업 브레인스토밍 플랫폼 API 문서")
        .version("1.0.0")
        .contact(new Contact()
            .name("1on-Musk Capstone Team")
            .url("https://github.com/1on-Musk-Capstone-Design")
            .email("contact@example.com")
        )
        .license(new License()
            .name("MIT License")
            .url("https://opensource.org/licenses/MIT")
        );
  }
}

