package com.ygyin.coop.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求封装类
 */
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 6824879573543519805L;
    /**
     * 用户帐号
     */
    private String userAccount;

    /**
     * 用户密码
     */
    private String userPassword;

    /**
     * 确认密码
     */
    private String checkedPassword;


}
