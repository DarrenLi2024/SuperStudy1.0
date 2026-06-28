package com.example.sys.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class UpdateUserStatusRequest {

    @NotNull(message = "状态不能为空")
    private Integer status;
}
