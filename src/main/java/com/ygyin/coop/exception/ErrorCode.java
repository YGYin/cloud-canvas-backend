package com.ygyin.coop.exception;

import lombok.Getter;

/**
 * 错误码枚举类
 */

@Getter
public enum ErrorCode {

    SUCCESS(0, "success"),
    PARAMS_ERROR(1, "params error"),
    NO_AUTH(3, "no authority"),
    NOT_LOGIN(401, "not login"),
    FORBIDDEN(403, "forbidden"),
    NOT_FOUND(404, "not found"),
    SYSTEM_ERROR(500, "system error"),
    OPERATION_ERROR(501, "operation error");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 异常信息
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
