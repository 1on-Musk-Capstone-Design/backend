package com.capstone.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(authz -> authz

            // Health check & Actuator
            .requestMatchers("/v1/health", "/actuator/**").permitAll()

            // Swagger/OpenAPI 문서
            .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

            // WebSocket
            .requestMatchers("/ws/**", "/app/**", "/topic/**", "/queue/**").permitAll()

            // Google OAuth (인증 필요 없음)
            .requestMatchers("/v1/auth-google/**").permitAll()

            // User info
            .requestMatchers("/v1/users/me").permitAll()

            // Workspace - 읽기는 허용, 쓰기는 인증 필요 (개발용: 임시로 모두 허용)
            .requestMatchers(HttpMethod.GET, "/v1/workspaces/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/v1/workspaces/**")
            .permitAll()  // TODO: 프로덕션에서는 authenticated()로 변경
            .requestMatchers(HttpMethod.PUT, "/v1/workspaces/**")
            .permitAll()   // TODO: 프로덕션에서는 authenticated()로 변경
            .requestMatchers(HttpMethod.DELETE, "/v1/workspaces/**")
            .permitAll()  // TODO: 프로덕션에서는 authenticated()로 변경

            // Canvas (개발용)
            .requestMatchers(HttpMethod.GET, "/v1/*/canvas").permitAll()
            .requestMatchers(HttpMethod.GET, "/v1/canvas/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/v1/*/canvas").permitAll()
            .requestMatchers(HttpMethod.PUT, "/v1/canvas/**").permitAll()
            .requestMatchers(HttpMethod.DELETE, "/v1/canvas/**").permitAll()

            // Idea (개발용)
            .requestMatchers(HttpMethod.GET, "/v1/ideas/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/v1/ideas/**").permitAll()
            .requestMatchers(HttpMethod.PUT, "/v1/ideas/**").permitAll()
            .requestMatchers(HttpMethod.DELETE, "/v1/ideas/**").permitAll()

            // Chat - 모두 허용 (개발용)
            .requestMatchers("/v1/chat/**").permitAll()  // TODO: 프로덕션에서는 authenticated()로 변경

            // VoiceSession - 모두 허용 (개발용)
            .requestMatchers("/v1/workspaces/*/voice/**")
            .permitAll()  // TODO: 프로덕션에서는 authenticated()로 변경

            // OpenAI (개발용)
            .requestMatchers("/v1/openai/**").permitAll()  // TODO: 프로덕션에서는 authenticated()로 변경

            // 나머지는 인증 필요
            .anyRequest().authenticated()
        );

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(Arrays.asList("*"));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
