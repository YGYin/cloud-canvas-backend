package com.ygyin.coop.controller;

import com.ygyin.coop.common.BaseResponse;
import com.ygyin.coop.common.ResUtils;
import com.ygyin.coop.exception.ErrorCode;
import com.ygyin.coop.exception.ThrowUtils;
import com.ygyin.coop.model.dto.UserRegisterRequest;
import com.ygyin.coop.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/user")
public class UserController {
    // 引入服务

    @Resource
    private UserService userService;

    /**
     * 用户注册
     * @param request 用户注册请求
     * @return 包含用户 id 的通用包装类
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest request) {
        // 对请求判空
        ThrowUtils.throwIf(request==null, ErrorCode.PARAMS_ERROR,"Controller: 注册请求为空");

        // 调用用户服务进行注册
        long userId = userService.userRegister(request);
        return ResUtils.success(userId);
    }
}
