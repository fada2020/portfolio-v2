package com.smartwork.dto.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.smartwork.domain.User;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * User response DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    private Long id;
    private String username;
    private String email;
    private String employeeId;
    private String name;
    private String department;
    private String position;
    private String phone;
    private User.UserStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private Set<String> roleNames;

    /**
     * Convert User entity to UserDto
     */
    public static UserDto fromEntity(User user) {
        if (user == null) {
            return null;
        }

        return UserDto.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .employeeId(user.getEmployeeId())
            .name(user.getName())
            .department(user.getDepartment())
            .position(user.getPosition())
            .phone(user.getPhone())
            .status(user.getStatus())
            .lastLoginAt(user.getLastLoginAt())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .roleNames(user.getRoles() != null
                ? user.getRoles().stream()
                    .map(role -> role.getRoleName())
                    .collect(Collectors.toSet())
                : null)
            .build();
    }
}
