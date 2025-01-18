package com.ygyin.coop.exception;

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
