package com.ygyin.coop.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用删除请求类，用于接收前端的删除请求
 */
@Data
public class DeleteRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}
