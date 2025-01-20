package com.ygyin.coop.controller;

import com.ygyin.coop.common.BaseResponse;
import com.ygyin.coop.common.ResUtils;
import com.ygyin.coop.exception.ErrorCode;
import com.ygyin.coop.exception.ThrowUtils;
import com.ygyin.coop.model.dto.UserLoginRequest;
import com.ygyin.coop.model.dto.UserRegisterRequest;
import com.ygyin.coop.model.entity.User;
import com.ygyin.coop.model.vo.LoginUserVO;
import com.ygyin.coop.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/user")
public class UserController {
    // 引入服务

    @Resource
    private UserService userService;

    /**
     * 用户注册
     *
     * @param registerRequest 用户注册请求
     * @return 包含用户 id 的通用包装类
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest registerRequest) {
        // 对请求判空
        ThrowUtils.throwIf(registerRequest == null, ErrorCode.PARAMS_ERROR,
                "Controller: 注册请求参数为空");

        // 调用用户服务进行注册
        long userId = userService.userRegister(registerRequest);
        return ResUtils.success(userId);
    }

    /**
     * 用户登录
     *
     * @param loginRequest 用户登录请求
     * @param request
     * @return 已脱敏的用户信息
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest loginRequest, HttpServletRequest request) {
        // 对请求判空
        ThrowUtils.throwIf(loginRequest == null, ErrorCode.PARAMS_ERROR,
                "Controller: 登录请求参数为空");

        // 调用用户服务进行注册
        LoginUserVO loginUserVO = userService.userLogin(loginRequest, request);
        return ResUtils.success(loginUserVO);
    }

    /**
     * 获取当前的登录用户
     *
     * @param request
     * @return
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        // 对请求判空
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR,
                "Controller: 获取登录用户失败");

        // 获取登录用户，对信息进行脱敏
        User loginUser = userService.getLoginUser(request);
        LoginUserVO loginUserVO = userService.getLoginUserVO(loginUser);
        return ResUtils.success(loginUserVO);
    }


    /**
     * 用户注销(动作)
     * @param request
     * @return
     */
    @PostMapping()
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        // 对请求判空
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR,
                "Controller: 用户注销请求失败");

        boolean isLogout = userService.userLogout(request);
        return ResUtils.success(isLogout);
    }
}
