package com.ygyin.coop.model.dto.areauser;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建空间用户关联请求
 */
@Data
public class AreaUserAddRequest implements Serializable {

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
