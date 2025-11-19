package com.smartwork.service;

import com.smartwork.domain.User;
import com.smartwork.dto.user.*;
import com.smartwork.exception.BusinessException;
import com.smartwork.exception.ErrorCode;
import com.smartwork.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for user management business logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Create a new user
     */
    @Transactional
    public UserDto createUser(UserCreateRequest request) {
        log.info("Creating new user: {}", request.getUsername());

        // Check duplicates
        validateUniqueConstraints(request.getUsername(), request.getEmail(), request.getEmployeeId());

        // Build user entity
        User user = User.builder()
            .username(request.getUsername())
            .password(passwordEncoder.encode(request.getPassword()))
            .email(request.getEmail())
            .employeeId(request.getEmployeeId())
            .name(request.getName())
            .department(request.getDepartment())
            .position(request.getPosition())
            .phone(request.getPhone())
            .status(User.UserStatus.ACTIVE)
            .failedLoginAttempts(0)
            .build();

        User savedUser = userRepository.save(user);
        log.info("User created successfully: id={}, username={}", savedUser.getId(), savedUser.getUsername());

        return UserDto.fromEntity(savedUser);
    }

    /**
     * Get user by ID
     */
    public UserDto getUserById(Long id) {
        log.debug("Finding user by id: {}", id);
        User user = findUserById(id);
        return UserDto.fromEntity(user);
    }

    /**
     * Get user by username
     */
    public UserDto getUserByUsername(String username) {
        log.debug("Finding user by username: {}", username);
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return UserDto.fromEntity(user);
    }

    /**
     * Get all users with pagination
     */
    public Page<UserDto> getAllUsers(Pageable pageable) {
        log.debug("Finding all users with pagination: page={}, size={}",
            pageable.getPageNumber(), pageable.getPageSize());
        return userRepository.findAll(pageable)
            .map(UserDto::fromEntity);
    }

    /**
     * Update user information
     */
    @Transactional
    public UserDto updateUser(Long id, UserUpdateRequest request) {
        log.info("Updating user: id={}", id);

        User user = findUserById(id);

        // Check email uniqueness if changed
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
            }
            user.setEmail(request.getEmail());
        }

        // Check employee ID uniqueness if changed
        if (request.getEmployeeId() != null && !request.getEmployeeId().equals(user.getEmployeeId())) {
            if (userRepository.existsByEmployeeId(request.getEmployeeId())) {
                throw new BusinessException(ErrorCode.DUPLICATE_EMPLOYEE_ID);
            }
            user.setEmployeeId(request.getEmployeeId());
        }

        // Update other fields
        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getDepartment() != null) {
            user.setDepartment(request.getDepartment());
        }
        if (request.getPosition() != null) {
            user.setPosition(request.getPosition());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully: id={}", updatedUser.getId());

        return UserDto.fromEntity(updatedUser);
    }

    /**
     * Change user password
     */
    @Transactional
    public void changePassword(Long id, UserPasswordChangeRequest request) {
        log.info("Changing password for user: id={}", id);

        // Validate password confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }

        User user = findUserById(id);

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.CURRENT_PASSWORD_INCORRECT);
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for user: id={}", id);
    }

    /**
     * Delete user (soft delete)
     */
    @Transactional
    public void deleteUser(Long id) {
        log.info("Deleting user: id={}", id);

        User user = findUserById(id);
        user.setIsDeleted(true);
        user.setStatus(User.UserStatus.RESIGNED);
        userRepository.save(user);

        log.info("User deleted successfully: id={}", id);
    }

    /**
     * Update user status
     */
    @Transactional
    public UserDto updateUserStatus(Long id, User.UserStatus status) {
        log.info("Updating user status: id={}, status={}", id, status);

        User user = findUserById(id);
        user.setStatus(status);

        // Reset failed login attempts if unlocking
        if (status == User.UserStatus.ACTIVE) {
            user.unlock();
        }

        User updatedUser = userRepository.save(user);
        log.info("User status updated successfully: id={}", updatedUser.getId());

        return UserDto.fromEntity(updatedUser);
    }

    /**
     * Check if username exists
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Check if email exists
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Check if employee ID exists
     */
    public boolean existsByEmployeeId(String employeeId) {
        return userRepository.existsByEmployeeId(employeeId);
    }

    /**
     * Find user by ID or throw exception
     */
    private User findUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * Validate unique constraints for username, email, and employee ID
     */
    private void validateUniqueConstraints(String username, String email, String employeeId) {
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (employeeId != null && userRepository.existsByEmployeeId(employeeId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMPLOYEE_ID);
        }
    }
}
