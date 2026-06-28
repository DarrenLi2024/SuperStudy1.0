package com.example.config;

import com.example.util.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseResult<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: {} - {}", request.getRequestURI(), e.getMessage());
        return ResponseResult.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseResult<Void> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.warn("权限不足: {} - {}", request.getRequestURI(), e.getMessage());
        return ResponseResult.forbidden("权限不足，请联系管理员");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseResult<Void> handleValidationException(MethodArgumentNotValidException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        StringBuilder message = new StringBuilder("参数校验失败: ");
        for (FieldError error : fieldErrors) {
            message.append(error.getField()).append(" ").append(error.getDefaultMessage()).append(", ");
        }
        return ResponseResult.badRequest(message.substring(0, message.length() - 2));
    }

    @ExceptionHandler(BindException.class)
    public ResponseResult<Void> handleBindException(BindException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        StringBuilder message = new StringBuilder("参数绑定失败: ");
        for (FieldError error : fieldErrors) {
            message.append(error.getField()).append(" ").append(error.getDefaultMessage()).append(", ");
        }
        return ResponseResult.badRequest(message.substring(0, message.length() - 2));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseResult<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常: {} - {}", request.getRequestURI(), e.getMessage(), e);
        return ResponseResult.error("系统内部错误，请稍后重试");
    }
}
