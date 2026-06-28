package com.example.sys.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserVO {

    private Long id;
    private String username;
    private String role;
    private Long studentId;
    private Integer status;
    private LocalDateTime createdAt;
}
