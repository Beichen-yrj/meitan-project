package com.meitan.controller;

import com.meitan.dto.*;
import com.meitan.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request,
                                            HttpServletRequest httpRequest) {
        try {
            LoginResponse resp = authService.login(
                    request,
                    resolveClientIp(httpRequest),
                    httpRequest.getHeader("User-Agent")
            );
            return ApiResponse.ok(resp);
        } catch (RuntimeException e) {
            return ApiResponse.error(401, e.getMessage());
        }
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@RequestBody RegisterRequest request) {
        try {
            authService.register(request);
            return ApiResponse.ok();
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return realIp == null || realIp.isBlank() ? request.getRemoteAddr() : realIp.trim();
    }
}
