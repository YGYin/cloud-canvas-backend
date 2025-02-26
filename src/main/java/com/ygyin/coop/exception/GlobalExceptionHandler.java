package com.ygyin.coop.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.ygyin.coop.common.BaseResponse;
import com.ygyin.coop.common.ResUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 用于捕获 Sa-Token 抛出的未登录异常，转换为自定义异常
     * @param e
     * @return
     */
    @ExceptionHandler(NotLoginException.class)
    public BaseResponse<?> notLoginException(NotLoginException e) {
        log.error("NotLoginException", e);
        return ResUtils.error(ErrorCode.NOT_LOGIN, e.getMessage());
    }

    /**
     * 用于捕获 Sa-Token 抛出的无权限异常，转换为自定义异常
     * @param e
     * @return
     */
    @ExceptionHandler(NotPermissionException.class)
    public BaseResponse<?> notPermissionExceptionHandler(NotPermissionException e) {
        log.error("NotPermissionException", e);
        return ResUtils.error(ErrorCode.NO_AUTH, e.getMessage());
    }


    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        // 打印日志，通过 ResUtils 返回 BaseResp
        log.error("BusinessException", e);
        return ResUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> businessExceptionHandler(RuntimeException e) {
        // 打印日志，通过 ResUtils 返回 BaseResp
        log.error("RuntimeException", e);
        return ResUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }
}
