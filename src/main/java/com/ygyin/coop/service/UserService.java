package com.ygyin.coop.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ygyin.coop.model.dto.user.UserLoginRequest;
import com.ygyin.coop.model.dto.user.UserQueryRequest;
import com.ygyin.coop.model.dto.user.UserRegisterRequest;
import com.ygyin.coop.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ygyin.coop.model.vo.LoginUserVO;
import com.ygyin.coop.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author yg
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2025-01-16 16:10:30
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param registerRequest 用户注册请求
     * @return 注册用户 id
     */
    long userRegister(UserRegisterRequest registerRequest);


    /**
     * 用户登录
     *
     * @param loginRequest 用户登录请求
     * @param request
     * @return 已脱敏的登录用户信息
     */
    LoginUserVO userLogin(UserLoginRequest loginRequest, HttpServletRequest request);

    /**
     * 获取登录用户(内部服务)，不直接返回给前端
     *
     * @param request
     * @return 用户信息(非VO)
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 当前用户注销
     *
     * @param request
     * @return 注销是否成功
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 加密用户密码
     *
     * @param password 用户密码
     * @return 加密后的用户密码
     */
    String getEncryptedPassword(String password);

    /**
     * 将 User 对象转换为 UserVO
     *
     * @param user 用户对象
     * @return 已脱敏的用户信息
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏后的 UserVO 列表
     *
     * @param userList 用户对象列表
     * @return 已脱敏的用户信息列表
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 将 User 对象转换为 LoginUserVO
     *
     * @param user 用户对象
     * @return 已脱敏的登录用户信息
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 根据查询用户的请求获取查询条件
     *
     * @param queryRequest
     * @return 查询条件对象
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest queryRequest);

    /**
     * 判断用户是否为管理员
     */
    boolean isAdmin(User user);
}
