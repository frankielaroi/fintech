package com.frankie.fintech.controller;

import com.frankie.fintech.dto.register.Request;
import com.frankie.fintech.dto.register.Response;
import com.frankie.fintech.dto.profile.PatchProfileRequest;
import com.frankie.fintech.dto.profile.PatchProfileResponse;
import com.frankie.fintech.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<Response> register(@Valid @RequestBody Request request) {
        log.info("Register attempt for email={}", maskEmail(request.getEmail()));

        Response response = userService.register(request);
        if (Boolean.TRUE.equals(response.getSuccess())) {
            log.info("Register success for email={}", maskEmail(request.getEmail()));
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        log.warn("Register rejected for email={} reason={}", maskEmail(request.getEmail()), response.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<com.frankie.fintech.dto.login.Response> login(
        @Valid @RequestBody com.frankie.fintech.dto.login.Request request,
        HttpServletRequest httpRequest
    ) {
        log.info("Login attempt for email={}", maskEmail(request.getEmail()));

        String ipAddress = extractClientIp(httpRequest);
        String userAgent = headerOrUnknown(httpRequest, "User-Agent");
        String forwardedFor = headerOrUnknown(httpRequest, "X-Forwarded-For");

        com.frankie.fintech.dto.login.Response response = userService.login(
            request,
            ipAddress,
            userAgent,
            forwardedFor
        );

        log.info("Login success for email={} ip={}", maskEmail(request.getEmail()), ipAddress);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/profile")
    public ResponseEntity<PatchProfileResponse> profile(
        @AuthenticationPrincipal UserDetails principal,
        @Valid @RequestBody PatchProfileRequest request,
        HttpServletRequest httpRequest
    ) {
        if (principal == null || principal.getUsername() == null || principal.getUsername().isBlank()) {
            throw new IllegalArgumentException("Authentication required");
        }

        String ipAddress = extractClientIp(httpRequest);
        String userAgent = headerOrUnknown(httpRequest, "User-Agent");
        String forwardedFor = headerOrUnknown(httpRequest, "X-Forwarded-For");

        PatchProfileResponse response = userService.patchProfile(
            principal.getUsername(),
            request,
            ipAddress,
            userAgent,
            forwardedFor
        );

        log.info("Profile patch success for email={} ip={}", maskEmail(principal.getUsername()), ipAddress);
        return ResponseEntity.ok(response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private String headerOrUnknown(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        return (value == null || value.isBlank()) ? "unknown" : value;
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "unknown";
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***";
        }

        return email.charAt(0) + "***" + email.substring(atIndex);
    }


}
