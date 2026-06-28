package com.example.sys.dto;

import lombok.Data;

@Data
public class LoginResponse {

    private String token;
    private String expireTime;
    private UserInfoVO user;
}
