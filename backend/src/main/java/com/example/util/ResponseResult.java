package com.example.util;

import lombok.Data;

import java.io.Serializable;

@Data
public class ResponseResult<T> implements Serializable {

    private Integer code;
    private String message;
    private T data;
    private Long timestamp;

    public ResponseResult() {
        this.timestamp = System.currentTimeMillis();
    }

    public ResponseResult(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> ResponseResult<T> success() {
        return new ResponseResult<>(200, "success", null);
    }

    public static <T> ResponseResult<T> success(T data) {
        return new ResponseResult<>(200, "success", data);
    }

    public static <T> ResponseResult<T> success(String message, T data) {
        return new ResponseResult<>(200, message, data);
    }

    public static <T> ResponseResult<T> error(String message) {
        return new ResponseResult<>(500, message, null);
    }

    public static <T> ResponseResult<T> error(Integer code, String message) {
        return new ResponseResult<>(code, message, null);
    }

    public static <T> ResponseResult<T> badRequest(String message) {
        return new ResponseResult<>(400, message, null);
    }

    public static <T> ResponseResult<T> unauthorized(String message) {
        return new ResponseResult<>(401, message, null);
    }

    public static <T> ResponseResult<T> forbidden(String message) {
        return new ResponseResult<>(403, message, null);
    }

    public static <T> ResponseResult<T> notFound(String message) {
        return new ResponseResult<>(404, message, null);
    }
}
