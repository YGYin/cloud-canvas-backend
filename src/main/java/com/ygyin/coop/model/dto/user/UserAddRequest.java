package com.ygyin.coop.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户新增请求封装类(管理员)
 */
@Data
public class UserAddRequest implements Serializable {

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 用户帐号
     */
    private String userAccount;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户权限
     */
    private String userRole;

    private static final long serialVersionUID = -1542846421856078863L;
}
