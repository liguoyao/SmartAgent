package com.smartagent.gateway.controller;

import com.smartagent.common.dto.ApiResponse;
import com.smartagent.common.dto.LoginRequest;
import com.smartagent.common.dto.LoginResponse;
import com.smartagent.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Authentication controller for the Gateway
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;
    private final ReactiveStringRedisTemplate redisTemplate;

    @PostMapping("/login")
    public Mono<ResponseEntity<ApiResponse<LoginResponse>>> login(@RequestBody LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());
        
        // In production, validate against user service/database
        // This is a simplified authentication for demonstration
        return validateCredentials(request.getUsername(), request.getPassword())
                .flatMap(isValid -> {
                    if (Boolean.TRUE.equals(isValid)) {
                        return generateTokens(request.getUsername());
                    } else {
                        return Mono.just(ResponseEntity.status(401)
                                .body(ApiResponse.unauthorized("Invalid credentials")));
                    }
                });
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<ApiResponse<LoginResponse>>> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        
        if (refreshToken == null || !jwtUtil.validateToken(refreshToken)) {
            return Mono.just(ResponseEntity.status(401)
                    .body(ApiResponse.unauthorized("Invalid refresh token")));
        }

        String username = jwtUtil.extractUsername(refreshToken);
        String role = jwtUtil.extractRole(refreshToken);
        Long userId = jwtUtil.extractUserId(refreshToken);

        String newAccessToken = jwtUtil.generateAccessToken(userId, username, role);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId, username, role);

        LoginResponse response = LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiration())
                .user(LoginResponse.UserInfo.builder()
                        .id(userId)
                        .username(username)
                        .role(role)
                        .build())
                .build();

        return Mono.just(ResponseEntity.ok(ApiResponse.success(response)));
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<ApiResponse<Void>>> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            // Add token to blacklist
            return redisTemplate.opsForValue()
                    .set("blacklist:" + token, "true")
                    .then(Mono.just(ResponseEntity.ok(ApiResponse.success("Logged out successfully"))))
                    .onErrorResume(e -> Mono.just(ResponseEntity.ok(ApiResponse.success("Logged out successfully"))));
        }
        return Mono.just(ResponseEntity.ok(ApiResponse.success("Logged out successfully")));
    }

    @GetMapping("/validate")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> validateToken(
            @RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.just(ResponseEntity.status(401)
                    .body(ApiResponse.unauthorized("Missing token")));
        }

        String token = authHeader.substring(7);
        
        if (!jwtUtil.validateToken(token)) {
            return Mono.just(ResponseEntity.status(401)
                    .body(ApiResponse.unauthorized("Invalid or expired token")));
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", jwtUtil.extractUserId(token));
        userInfo.put("username", jwtUtil.extractUsername(token));
        userInfo.put("role", jwtUtil.extractRole(token));

        return Mono.just(ResponseEntity.ok(ApiResponse.success(userInfo)));
    }

    private Mono<Boolean> validateCredentials(String username, String password) {
        // In production, call user service or validate against database
        // This is a mock validation for demonstration
        boolean isValid = "admin".equals(username) && "admin123".equals(password);
        return Mono.just(isValid);
    }

    private Mono<ResponseEntity<ApiResponse<LoginResponse>>> generateTokens(String username) {
        // Mock user data - in production, fetch from user service
        Long userId = 1L;
        String role = "ADMIN";

        String accessToken = jwtUtil.generateAccessToken(userId, username, role);
        String refreshToken = jwtUtil.generateRefreshToken(userId, username, role);

        LoginResponse response = LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiration())
                .user(LoginResponse.UserInfo.builder()
                        .id(userId)
                        .username(username)
                        .nickname("Administrator")
                        .role(role)
                        .build())
                .build();

        log.info("Login successful for user: {}", username);
        return Mono.just(ResponseEntity.ok(ApiResponse.success("Login successful", response)));
    }
}
