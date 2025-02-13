package com.ygyin.coop.model.dto.area;

import com.ygyin.coop.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询空间请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AreaQueryRequest extends PageRequest implements Serializable {

    /**
     * 空间 id
     */
    private Long id;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 空间名称
     */
    private String areaName;

    /**
     * 空间级别：0-default 1-Pro 2-Ultra
     */
    private Integer areaLevel;


    private static final long serialVersionUID = 1L;
}
