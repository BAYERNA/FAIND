package com.faind.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

  private static final String BEARER_SCHEME = "bearerAuth";

  @Bean
  public OpenAPI faindOpenApi() {
    return new OpenAPI()
        .info(new Info().title("FAIND API").version("v1")
            .description("AI가 화재를 감지하고 골든타임을 사수하는 지능형 소방 대응 시스템 — 핵심 도메인 API"))
        .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
        .components(new Components()
            .addSecuritySchemes(BEARER_SCHEME,
                new SecurityScheme()
                    .name(BEARER_SCHEME)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
  }
}
