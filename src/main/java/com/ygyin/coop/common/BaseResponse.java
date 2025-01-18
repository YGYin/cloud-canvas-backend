package com.ygyin.coop.common;


import com.ygyin.coop.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 全局通用响应封装类
 *
 * @param <T>
 */
@Data
public class BaseResponse<T> implements Serializable {

    private int code;

    private T data;

    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    /**
     * 根据错误码获取 code 及 msg 封装响应
     *
     * @param errorCode
     */
    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
