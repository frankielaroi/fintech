package com.frankie.fintech.service;

import com.frankie.fintech.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAuditService {

    private final UserRepository userRepository;

    @Async
    @Transactional
    public void recordSuccessfulLogin(
        UUID userId,
        String ipAddress,
        String userAgent,
        String forwardedFor,
        LocalDateTime loginTime
    ) {
        try {
            userRepository.updateLastLogin(userId, loginTime, ipAddress);

            userRepository.findById(userId).ifPresent(user -> {
                user.setMetadata(buildLoginMetadata(ipAddress, userAgent, forwardedFor, loginTime));
                userRepository.save(user);
            });
        } catch (Exception ex) {
            // Never block login flow because of audit persistence failures.
            log.warn("Failed to persist async login audit for userId={} ip={}", userId, ipAddress, ex);
        }
    }

    @Async
    public void recordProfileUpdate(
        UUID userId,
        String ipAddress,
        String userAgent,
        String forwardedFor,
        LocalDateTime eventTime
    ) {
        try {
            log.info(
                "Profile updated userId={} ip={} userAgent={} forwardedFor={} at={}",
                userId,
                ipAddress,
                userAgent,
                forwardedFor,
                eventTime
            );
        } catch (Exception ex) {
            // Never block profile updates because of audit logging failures.
            log.warn("Failed to record async profile update audit for userId={}", userId, ex);
        }
    }

    private String buildLoginMetadata(
        String ipAddress,
        String userAgent,
        String forwardedFor,
        LocalDateTime loginTime
    ) {
        return String.format(
            "{\"lastLoginIp\":\"%s\",\"lastLoginUserAgent\":\"%s\",\"lastLoginForwardedFor\":\"%s\",\"lastLoginAt\":\"%s\"}",
            safe(ipAddress),
            safe(userAgent),
            safe(forwardedFor),
            loginTime
        );
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
