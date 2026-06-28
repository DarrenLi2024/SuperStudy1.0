package com.example.sys.dto;

import lombok.Data;

@Data
public class UserInfoVO {

    private Long id;
    private String username;
    private String role;
    private Long studentId;
    private Integer status;
}
