package com.example.gateway.config;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${security.jwt.secret}")
    private String secret;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 公开路径
                        .pathMatchers("/api/auth/**", "/actuator/**").permitAll()
                        // 其他所有请求需要认证
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtDecoder(jwtDecoder()))
                );

        return http.build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        try {
            // 将字符串转换为 SecretKey
            // 注意：secret 必须足够长 (HS256 >= 32 bytes)
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

            // 创建并返回 NimbusReactiveJwtDecoder
            return NimbusReactiveJwtDecoder.withSecretKey(key).build();
        } catch (Exception e) {
            // 如果密钥格式不正确，抛出更明确的异常
            throw new IllegalStateException("Invalid JWT Secret Key configuration. Ensure it is at least 32 characters long for HS256.", e);
        }
    }
}