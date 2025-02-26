package com.ygyin.coop.model.dto.areauser;

import lombok.Data;

import java.io.Serializable;

/**
 * 编辑空间用户关联请求
 */
@Data
public class AreaUserEditRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String areaRole;

    private static final long serialVersionUID = 1L;
}
