package com.ygyin.coop.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ygyin.coop.annotation.AuthVerify;
import com.ygyin.coop.common.BaseResponse;
import com.ygyin.coop.common.DeleteRequest;
import com.ygyin.coop.common.ResUtils;
import com.ygyin.coop.constant.UserConstant;
import com.ygyin.coop.exception.ErrorCode;
import com.ygyin.coop.exception.ThrowUtils;
import com.ygyin.coop.model.dto.user.*;
import com.ygyin.coop.model.entity.User;
import com.ygyin.coop.model.vo.LoginUserVO;
import com.ygyin.coop.model.vo.UserVO;
import com.ygyin.coop.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

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
     *
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

    /**
     * 用户创建(管理员)
     *
     * @param addRequest 创建用户请求
     * @return 包含用户 id 的通用包装类
     */
    @PostMapping("/add")
    @AuthVerify(requiredRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> adduser(@RequestBody UserAddRequest addRequest) {
        // 对请求判空
        ThrowUtils.throwIf(addRequest == null, ErrorCode.PARAMS_ERROR,
                "Controller: 用户创建请求参数为空");

        // 新建用户对象，将 addRequest 转化为 user 对象
        User user = new User();
        BeanUtil.copyProperties(addRequest, user);
        // 补充 user 对象的初始默认密码
        final String DEFAULT_PW = "12345678";
        user.setUserPassword(userService.getEncryptedPassword(DEFAULT_PW));

        // 将 user 保存到数据库，未成功保存到数据库则抛业务异常
        boolean isSave = userService.save(user);
        ThrowUtils.throwIf(!isSave, ErrorCode.OPERATION_ERROR,
                "Controller: 创建的用户未成功保存到数据库中");

        return ResUtils.success(user.getId());
    }

    /**
     * 根据用户 id 获取用户
     *
     * @param id
     * @return 包含用户的通用包装类
     */
    @GetMapping("/get")
    @AuthVerify(requiredRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        // 先检查 id 是否合法
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR,
                "Controller: 根据 id 获取用户 id 不合法");

        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND,
                "Controller: 根据 id 获取的用户不存在");

        return ResUtils.success(user);
    }

    /**
     * 根据用户 id 获取用户 vo
     *
     * @param id
     * @return 包含用户 vo 的通用包装类
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        // 直接调用 getUserById，再转为 userVO
        BaseResponse<User> baseResponse = getUserById(id);
        User user = baseResponse.getData();

        return ResUtils.success(userService.getUserVO(user));
    }

    /**
     * 删除用户
     *
     * @param deleteRequest 删除用户请求
     * @return 删除是否成功
     */
    @PostMapping("/delete")
    @AuthVerify(requiredRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        // 先检查删除请求是否为空，id 是否合法
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR, "Controller: 删除用户 id 不合法");

        boolean isDelete = userService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!isDelete, ErrorCode.OPERATION_ERROR,
                "Controller: 数据库未成功删除该用户");

        return ResUtils.success(isDelete);
    }

    /**
     * 更新用户
     *
     * @param updateRequest 更新用户请求
     * @return 删除是否成功
     */
    @PostMapping("/update")
    @AuthVerify(requiredRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest updateRequest) {
        // 先检查删除请求是否为空，id 是否合法
        ThrowUtils.throwIf(updateRequest == null || updateRequest.getId() == null,
                ErrorCode.PARAMS_ERROR, "Controller: 更新用户 id 不合法");


        // 将用户更新请求转化为 user 对象，根据用户 id 更新数据库
        User user = new User();
        BeanUtil.copyProperties(updateRequest, user);

        boolean isUpdate = userService.updateById(user);
        ThrowUtils.throwIf(!isUpdate, ErrorCode.OPERATION_ERROR,
                "Controller: 数据库未成功更新该用户信息");

        return ResUtils.success(isUpdate);
    }

    /**
     * 分页获取脱敏后的用户列表(管理员)
     *
     * @param queryRequest
     * @return 脱敏后的用户信息页面
     */
    @PostMapping("/list/page/vo")
    @AuthVerify(requiredRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest queryRequest) {
        // 对分页查询判空
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR
                , "Controller: 分页查询用户请求为空");

        // 获取分页查询用户请求的当前页面和 size
        long currentPage = queryRequest.getCurrentPage();
        long pageSize = queryRequest.getPageSize();

        // 调用 MyBatis 中 page 来将获取的结果进行分页，
        // 传入 Page 对象和 QueryWrapper 查询条件对象
        Page<User> userPage = userService.page(new Page<>(currentPage, pageSize),
                userService.getQueryWrapper(queryRequest));
        // 将 userPage 信息脱敏转化为 UserVO page，先根据查询到的总数新建 page 对象
        Page<UserVO> userVOPage = new Page<>(currentPage, pageSize, userPage.getTotal());
        // 调用 user service 获得 userVO 分页列表，用于保存到当前空白的 userVO page 中
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);

        // 返回 userVO Page
        return ResUtils.success(userVOPage);
    }
}
