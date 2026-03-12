package com.frankie.fintech.service;

import com.frankie.fintech.dto.register.Request;
import com.frankie.fintech.dto.register.Response;
import com.frankie.fintech.dto.profile.PatchProfileRequest;
import com.frankie.fintech.dto.profile.PatchProfileResponse;
import com.frankie.fintech.entity.User;
import com.frankie.fintech.entity.Wallet;
import com.frankie.fintech.repository.UserRepository;
import com.frankie.fintech.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LoginAuditService loginAuditService;

    @Transactional
    public Response register(Request request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return Response.failure("Email already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(User.UserStatus.ACTIVE);
        user.setRoles(Set.of(User.UserRole.ROLE_USER));

        User savedUser = userRepository.save(user);

        Wallet defaultWallet = Wallet.builder()
            .user(savedUser)
            .walletType(Wallet.WalletType.PRIMARY)
            .status(Wallet.WalletStatus.ACTIVE)
            .isDefault(true)
            .build();
        walletRepository.save(defaultWallet);

        return Response.success("User registered successfully");
    }

    public com.frankie.fintech.dto.login.Response login(
        com.frankie.fintech.dto.login.Request request,
        String ipAddress,
        String userAgent,
        String forwardedFor
    ) {
        if (request.getEmail() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("Email and password must be provided");
        }

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (AuthenticationException ex) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Fire-and-forget login audit; failures are handled inside async service.
        loginAuditService.recordSuccessfulLogin(
            user.getId(),
            ipAddress,
            userAgent,
            forwardedFor,
            LocalDateTime.now()
        );

        return com.frankie.fintech.dto.login.Response.from(accessToken, refreshToken, user);
    }

    @Transactional
    public PatchProfileResponse patchProfile(
        String authenticatedEmail,
        PatchProfileRequest request,
        String ipAddress,
        String userAgent,
        String forwardedFor
    ) {
        if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
            throw new IllegalArgumentException("Authentication required");
        }

        User user = userRepository.findByEmailWithLock(authenticatedEmail)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean changed = false;

        if (request.getName() != null) {
            String normalizedName = request.getName().trim();
            if (normalizedName.isEmpty()) {
                throw new IllegalArgumentException("Name cannot be blank");
            }
            if (!normalizedName.equals(user.getName())) {
                user.setName(normalizedName);
                changed = true;
            }
        }

        if (request.getPhoneNumber() != null) {
            String normalizedPhone = request.getPhoneNumber().trim();
            if (!normalizedPhone.equals(user.getPhoneNumber())) {
                if (userRepository.existsByPhoneNumber(normalizedPhone)) {
                    throw new IllegalStateException("Phone number is already in use");
                }
                user.setPhoneNumber(normalizedPhone);
                changed = true;
            }
        }

        if (!changed) {
            return PatchProfileResponse.from(user);
        }

        User savedUser = userRepository.save(user);
        loginAuditService.recordProfileUpdate(savedUser.getId(), ipAddress, userAgent, forwardedFor, LocalDateTime.now());

        return PatchProfileResponse.from(savedUser);
    }
}
