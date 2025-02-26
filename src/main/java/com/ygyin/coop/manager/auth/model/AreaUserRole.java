package com.ygyin.coop.manager.auth.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 空间成员角色定义实体类
 */
@Data
public class AreaUserRole implements Serializable {

    /**
     * 角色键
     */
    private String key;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 权限键列表
     */
    private List<String> permissions;

    /**
     * 角色描述
     */
    private String desc;

    private static final long serialVersionUID = 1L;

}
