package com.ygyin.coop.service;

import com.ygyin.coop.model.dto.UserRegisterRequest;
import com.ygyin.coop.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author yg
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-01-16 16:10:30
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     * @param userRegisterRequest 用户注册请求
     * @return 注册用户 id
     */
    long userRegister(UserRegisterRequest userRegisterRequest);


    /**
     * 加密用户密码
     * @param password 用户密码
     * @return 加密后的用户密码
     */
    String getEncryptedPassword(String password);
}
