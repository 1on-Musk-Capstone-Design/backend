package com.capstone.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("Capstone Project API")
            .version("1.0.0")
            .description("캡스톤 프로젝트 REST API 문서")
            .contact(new Contact()
                .name("Capstone Team")
                .email("capstone@example.com")))
        .servers(List.of(
            new Server().url("http://localhost:8080/api").description("로컬 개발 서버"),
            new Server().url("https://your-production-url.com/api").description("프로덕션 서버")
        ));
  }
}

