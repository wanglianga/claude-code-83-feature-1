package com.community.haircut.security;

import com.community.haircut.enums.Role;
import lombok.Data;

@Data
public class LoginUser {

    private Long userId;
    private String username;
    private String realName;
    private Role role;
}
