package com.xiaozhi.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.xiaozhi.common.web.AjaxResult;

/**
 * 全局异常处理器
 * 
 * @author Joey
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 用户名不存在异常
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AjaxResult handleUsernameNotFoundException(UsernameNotFoundException e, WebRequest request) {
        logger.error(e.getMessage(), e);
        return AjaxResult.error("用户名不存在");
    }

    /**
     * 用户密码不匹配异常
     */
    @ExceptionHandler(UserPasswordNotMatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AjaxResult handleUserPasswordNotMatchException(UserPasswordNotMatchException e, WebRequest request) {
        logger.error(e.getMessage(), e);
        return AjaxResult.error("用户密码不正确");
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public AjaxResult handleException(Exception e, WebRequest request) {
        logger.error(e.getMessage(), e);
        return AjaxResult.error("服务器错误，请联系管理员");
    }
}