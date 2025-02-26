package com.ygyin.coop.manager.auth.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间成员权限定义实体类
 */
@Data
public class AreaUserPermission implements Serializable {

    /**
     * 权限键
     */
    private String key;

    /**
     * 权限名称
     */
    private String name;

    /**
     * 权限描述
     */
    private String desc;

    private static final long serialVersionUID = 1L;

}

