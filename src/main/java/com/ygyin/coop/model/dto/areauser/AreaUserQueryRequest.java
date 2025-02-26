package com.ygyin.coop.model.dto.areauser;

import lombok.Data;

import java.io.Serializable;

/**
 * 查询空间用户关联请求
 */
@Data
public class AreaUserQueryRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 空间 id
     */
    private Long areaId;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String areaRole;

    private static final long serialVersionUID = 1L;
}

