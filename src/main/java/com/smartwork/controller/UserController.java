package com.smartwork.controller;

import com.smartwork.domain.User;
import com.smartwork.dto.ApiResponse;
import com.smartwork.dto.user.*;
import com.smartwork.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for user management operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User management APIs")
public class UserController {

    private final UserService userService;

    /**
     * Create a new user
     */
    @PostMapping
    @Operation(summary = "Create user", description = "Create a new user account")
    public ResponseEntity<ApiResponse<UserDto>> createUser(
        @Valid @RequestBody UserCreateRequest request
    ) {
        log.info("REST request to create user: {}", request.getUsername());
        UserDto userDto = userService.createUser(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success("User created successfully", userDto));
    }

    /**
     * Get user by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get user", description = "Get user by ID")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(
        @Parameter(description = "User ID") @PathVariable Long id
    ) {
        log.info("REST request to get user: id={}", id);
        UserDto userDto = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(userDto));
    }

    /**
     * Get user by username
     */
    @GetMapping("/username/{username}")
    @Operation(summary = "Get user by username", description = "Get user by username")
    public ResponseEntity<ApiResponse<UserDto>> getUserByUsername(
        @Parameter(description = "Username") @PathVariable String username
    ) {
        log.info("REST request to get user by username: {}", username);
        UserDto userDto = userService.getUserByUsername(username);
        return ResponseEntity.ok(ApiResponse.success(userDto));
    }

    /**
     * Get all users with pagination
     */
    @GetMapping
    @Operation(summary = "Get all users", description = "Get all users with pagination")
    public ResponseEntity<ApiResponse<Page<UserDto>>> getAllUsers(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("REST request to get all users: page={}, size={}",
            pageable.getPageNumber(), pageable.getPageSize());
        Page<UserDto> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    /**
     * Update user information
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Update user information")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
        @Parameter(description = "User ID") @PathVariable Long id,
        @Valid @RequestBody UserUpdateRequest request
    ) {
        log.info("REST request to update user: id={}", id);
        UserDto userDto = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", userDto));
    }

    /**
     * Change user password
     */
    @PutMapping("/{id}/password")
    @Operation(summary = "Change password", description = "Change user password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
        @Parameter(description = "User ID") @PathVariable Long id,
        @Valid @RequestBody UserPasswordChangeRequest request
    ) {
        log.info("REST request to change password: id={}", id);
        userService.changePassword(id, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    /**
     * Update user status
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "Update user status", description = "Update user status (ACTIVE, INACTIVE, LOCKED, etc.)")
    public ResponseEntity<ApiResponse<UserDto>> updateUserStatus(
        @Parameter(description = "User ID") @PathVariable Long id,
        @Parameter(description = "User status") @RequestParam User.UserStatus status
    ) {
        log.info("REST request to update user status: id={}, status={}", id, status);
        UserDto userDto = userService.updateUserStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("User status updated successfully", userDto));
    }

    /**
     * Delete user (soft delete)
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Delete user (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
        @Parameter(description = "User ID") @PathVariable Long id
    ) {
        log.info("REST request to delete user: id={}", id);
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully"));
    }

    /**
     * Check username availability
     */
    @GetMapping("/check/username")
    @Operation(summary = "Check username", description = "Check if username is available")
    public ResponseEntity<ApiResponse<Boolean>> checkUsername(
        @Parameter(description = "Username to check") @RequestParam String username
    ) {
        log.debug("REST request to check username: {}", username);
        boolean exists = userService.existsByUsername(username);
        return ResponseEntity.ok(ApiResponse.success(!exists));
    }

    /**
     * Check email availability
     */
    @GetMapping("/check/email")
    @Operation(summary = "Check email", description = "Check if email is available")
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(
        @Parameter(description = "Email to check") @RequestParam String email
    ) {
        log.debug("REST request to check email: {}", email);
        boolean exists = userService.existsByEmail(email);
        return ResponseEntity.ok(ApiResponse.success(!exists));
    }

    /**
     * Check employee ID availability
     */
    @GetMapping("/check/employee-id")
    @Operation(summary = "Check employee ID", description = "Check if employee ID is available")
    public ResponseEntity<ApiResponse<Boolean>> checkEmployeeId(
        @Parameter(description = "Employee ID to check") @RequestParam String employeeId
    ) {
        log.debug("REST request to check employee ID: {}", employeeId);
        boolean exists = userService.existsByEmployeeId(employeeId);
        return ResponseEntity.ok(ApiResponse.success(!exists));
    }
}
