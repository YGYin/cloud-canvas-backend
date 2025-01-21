package com.ygyin.coop.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求封装类
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 1424837483102102018L;

    /**
     * 用户帐号
     */
    private String userAccount;

    /**
     * 用户密码
     */
    private String userPassword;
}
