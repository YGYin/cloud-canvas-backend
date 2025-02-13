package com.ygyin.coop.model.dto.area;

import lombok.Data;

import java.io.Serializable;

/**
 * 编辑个人空间请求
 */
@Data
public class AreaEditRequest implements Serializable {

    /**
     * 空间 id
     */
    private Long id;

    /**
     * 个人空间名称
     */
    private String areaName;

    private static final long serialVersionUID = 1L;
}
