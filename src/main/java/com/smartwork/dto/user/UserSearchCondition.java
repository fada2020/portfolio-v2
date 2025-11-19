package com.smartwork.dto.user;

import com.smartwork.domain.User;
import lombok.*;

/**
 * Search condition for user queries
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSearchCondition {

    private String username;
    private String email;
    private String employeeId;
    private String name;
    private String department;
    private String position;
    private User.UserStatus status;
}
