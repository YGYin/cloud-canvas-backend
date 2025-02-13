package com.ygyin.coop.model.dto.area;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新空间请求（管理员）
 */
@Data
public class AreaUpdateRequest implements Serializable {

    /**
     * 空间 id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String areaName;

    /**
     * 空间级别：0-default 1-Pro 2-Ultra
     */
    private Integer areaLevel;

    /**
     * 空间容量最大限制
     */
    private Long maxSize;

    /**
     * 空间图片最大数量
     */
    private Long maxNum;

    private static final long serialVersionUID = 1L;
}
