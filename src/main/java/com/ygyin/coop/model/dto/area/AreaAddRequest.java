package com.ygyin.coop.model.dto.area;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建个人空间请求
 */
@Data
public class AreaAddRequest implements Serializable {

    /**
     * 个人空间名称
     */
    private String areaName;

    /**
     * 空间级别：0-default 1-Pro 2-Ultra
     */
    private Integer areaLevel;

    private static final long serialVersionUID = 1L;
}
