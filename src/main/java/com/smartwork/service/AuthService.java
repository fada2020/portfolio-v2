package com.smartwork.service;

import com.smartwork.domain.User;
import com.smartwork.dto.auth.LoginRequest;
import com.smartwork.dto.auth.LoginResponse;
import com.smartwork.dto.user.UserCreateRequest;
import com.smartwork.dto.user.UserDto;
import com.smartwork.exception.BusinessException;
import com.smartwork.exception.ErrorCode;
import com.smartwork.repository.UserRepository;
import com.smartwork.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for authentication operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * User login
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt: username={}", request.getUsername());

        // Find user
        User user = userRepository.findByUsernameWithRoles(request.getUsername())
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        // Check if account is locked
        if (user.isLocked()) {
            log.warn("Login failed - account locked: username={}", request.getUsername());
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        // Check if account is active
        if (!user.isActive()) {
            log.warn("Login failed - account not active: username={}, status={}",
                request.getUsername(), user.getStatus());
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed - invalid password: username={}", request.getUsername());
            user.incrementFailedAttempts();
            userRepository.save(user);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // Update last login time
        user.updateLastLogin();
        userRepository.save(user);

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(user.getUsername());
        Long expiresIn = jwtTokenProvider.getExpirationTime();

        log.info("Login successful: username={}", request.getUsername());

        return LoginResponse.of(token, expiresIn, UserDto.fromEntity(user));
    }

    /**
     * User registration
     */
    @Transactional
    public UserDto register(UserCreateRequest request) {
        log.info("Registration attempt: username={}", request.getUsername());
        return userService.createUser(request);
    }
}
