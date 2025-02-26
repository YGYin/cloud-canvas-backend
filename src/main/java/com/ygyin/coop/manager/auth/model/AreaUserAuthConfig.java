package com.ygyin.coop.manager.auth.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 空间成员权限及角色全局配置实体类
 */
@Data
public class AreaUserAuthConfig implements Serializable {

    /**
     * 空间成员的权限定义列表，对应增删改查及管理用户权限
     */
    private List<AreaUserPermission> permissions;

    /**
     * 空间成员角色定义列表，用于对应不同权限
     */
    private List<AreaUserRole> roles;

    private static final long serialVersionUID = 1L;
}
