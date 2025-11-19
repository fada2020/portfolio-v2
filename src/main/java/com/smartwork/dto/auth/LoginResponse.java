package com.smartwork.dto.auth;

import com.smartwork.dto.user.UserDto;
import lombok.*;

/**
 * Response DTO for successful login
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String accessToken;
    private String tokenType;
    private Long expiresIn;
    private UserDto user;

    public static LoginResponse of(String accessToken, Long expiresIn, UserDto user) {
        return LoginResponse.builder()
            .accessToken(accessToken)
            .tokenType("Bearer")
            .expiresIn(expiresIn)
            .user(user)
            .build();
    }
}
