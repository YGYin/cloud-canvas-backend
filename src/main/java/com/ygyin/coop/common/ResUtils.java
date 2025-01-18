package com.ygyin.coop.common;

import com.ygyin.coop.exception.ErrorCode;

/**
 * 响应结果工具类
 */
public class ResUtils {

    /**
     * 成功请求
     *
     * @param data
     * @param <T>
     * @return 通用响应
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, data, "success");
    }

    /**
     * 失败请求
     *
     * @param code 错误码
     * @param message  错误消息
     * @return 通用响应
     */
    public static BaseResponse<?> error(int code, String message) {
        return new BaseResponse<>(code, null, message);
    }

    /**
     * 失败请求
     *
     * @param errorCode 错误码
     * @return 通用响应
     */
    public static BaseResponse<?> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    public static BaseResponse<?> error(ErrorCode errorCode, String message) {
        return new BaseResponse<>(errorCode.getCode(), null, message);
    }
}
