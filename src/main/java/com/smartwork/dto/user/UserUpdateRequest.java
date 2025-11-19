package com.smartwork.dto.user;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Request DTO for updating user information
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequest {

    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Size(max = 100, message = "이메일은 100자를 초과할 수 없습니다")
    private String email;

    @Size(max = 20, message = "사번은 20자를 초과할 수 없습니다")
    private String employeeId;

    @Size(min = 2, max = 50, message = "이름은 2-50자 사이여야 합니다")
    private String name;

    @Size(max = 50, message = "부서명은 50자를 초과할 수 없습니다")
    private String department;

    @Size(max = 50, message = "직책은 50자를 초과할 수 없습니다")
    private String position;

    @Pattern(
        regexp = "^(\\d{2,3}-\\d{3,4}-\\d{4})?$",
        message = "전화번호 형식이 올바르지 않습니다 (예: 010-1234-5678)"
    )
    private String phone;
}
