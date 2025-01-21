package com.ygyin.coop.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ygyin.coop.constant.UserConstant;
import com.ygyin.coop.exception.BusinessException;
import com.ygyin.coop.exception.ErrorCode;
import com.ygyin.coop.exception.ThrowUtils;
import com.ygyin.coop.mapper.UserMapper;
import com.ygyin.coop.model.dto.user.UserLoginRequest;
import com.ygyin.coop.model.dto.user.UserQueryRequest;
import com.ygyin.coop.model.dto.user.UserRegisterRequest;
import com.ygyin.coop.model.entity.User;
import com.ygyin.coop.model.enums.UserRoleEnum;
import com.ygyin.coop.model.vo.LoginUserVO;
import com.ygyin.coop.model.vo.UserVO;
import com.ygyin.coop.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author yg
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-01-16 16:10:30
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {


    private final String SALT = "yg";

    /**
     * 用户注册
     *
     * @param registerRequest 用户注册请求
     * @return 注册用户 id
     */
    @Override
    public long userRegister(UserRegisterRequest registerRequest) {
        // 1. 校验请求参数是否为空，长度是否符合标准，确认密码是否一致
        ThrowUtils.throwIf(registerRequest == null, ErrorCode.PARAMS_ERROR, "注册请求对象为空");
        // 获取各项属性
        String userAccount = registerRequest.getUserAccount();
        String userPassword = registerRequest.getUserPassword();
        String checkedPassword = registerRequest.getCheckedPassword();

        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword, checkedPassword),
                ErrorCode.PARAMS_ERROR, "注册请求参数为空");

        ThrowUtils.throwIf(userAccount.length() < 3,
                ErrorCode.PARAMS_ERROR, "用户帐号长度过短");

        ThrowUtils.throwIf(userPassword.length() < 8 || checkedPassword.length() < 8,
                ErrorCode.PARAMS_ERROR, "密码长度过短");

        ThrowUtils.throwIf(!userPassword.equals(checkedPassword),
                ErrorCode.PARAMS_ERROR, "密码和确认密码不一致");

        // 2. 检查用于注册的帐号是否和数据库已有帐号重复，
        // 通过 queryWrapper 定义查询条件，使用 baseMapper 应用条件操作数据库返回结果
        // 此处可以不加事务，因为 user 表中对 userAccount 已有唯一索引
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        // 符合记录数大于 0 说明帐号已存在
        long countNum = this.baseMapper.selectCount(queryWrapper);
        ThrowUtils.throwIf(countNum > 0, ErrorCode.PARAMS_ERROR, "当前帐号已注册");

        // 3. 密码进行加密，不能明文储存
        String encryptedPassword = getEncryptedPassword(userPassword);

        // 4. 封装新对象写入到数据库中
        User user = new User();
        user.setUserName("NoName");
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptedPassword);
        user.setUserRole(UserRoleEnum.USER.getVal());

        boolean isSaved = this.save(user);
        ThrowUtils.throwIf(!isSaved, ErrorCode.SYSTEM_ERROR, "数据库错误，注册失败");

        // 返回注册用户的 id，MyBatis 已在 save() 中做了主键回填
        return user.getId();
    }

    /**
     * 用户登录
     *
     * @param loginRequest 用户登录请求
     * @param request
     * @return 已脱敏的登录用户信息
     */
    @Override
    public LoginUserVO userLogin(UserLoginRequest loginRequest, HttpServletRequest request) {
        // 1. 校验参数
        ThrowUtils.throwIf(loginRequest == null, ErrorCode.PARAMS_ERROR, "注册请求对象为空");
        // 获取各项属性
        String userAccount = loginRequest.getUserAccount();
        String userPassword = loginRequest.getUserPassword();

        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword),
                ErrorCode.PARAMS_ERROR, "登录请求参数为空");

        ThrowUtils.throwIf(userAccount.length() < 3 || userPassword.length() < 8,
                ErrorCode.PARAMS_ERROR, "用户帐号或密码非法");

        // 2. 登录请求中的密码加密，并使用 queryWrapper 查询数据库中用户是否存在，密码是否匹配
        String encryptedPassword = getEncryptedPassword(userPassword);

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptedPassword);
        User selectedUser = this.baseMapper.selectOne(queryWrapper);

        // 如果不存在，记录日志抛出异常
        if (selectedUser == null) {
            log.info("User log in failed, wrong account or password");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该用户名不存在或密码错误");
        }

        // 3. 保存用户的登录状态，并返回脱敏后的用户信息
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, selectedUser);
        return getLoginUserVO(selectedUser);
    }

    /**
     * 获取登录用户(内部服务)，不直接返回给前端
     *
     * @param request
     * @return 用户信息(非VO)
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 1. 从 session 中获取 Attribute，获得用户信息判断用户是否登录
        User curUser = (User) request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        // 未登录抛业务异常
        ThrowUtils.throwIf(curUser == null || curUser.getId() == null, ErrorCode.NOT_LOGIN);

        // 2. 已登录，因为 session 中为缓存，应该通过当前用户 id 查数据库获取最新的当前用户
        curUser = this.getById(curUser.getId());
        // 为空抛业务异常
        ThrowUtils.throwIf(curUser == null, ErrorCode.NOT_LOGIN,
                "获取登录用户失败，数据库中未查询到该用户最新信息");

        return curUser;
    }

    /**
     * 当前用户注销
     *
     * @param request
     * @return 注销是否成功
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 1. 从 session 中获取 Attribute，获得用户信息判断用户是否登录
        Object userObject = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        // 未登录抛业务异常，为操作错误
        ThrowUtils.throwIf(userObject == null, ErrorCode.OPERATION_ERROR,
                "注销失败，当前用户未登录或不存在");

        // 2. 当前用户已登录，移除其登录状态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);

        return true;
    }

    /**
     * 加密用户密码
     *
     * @param password 用户密码
     * @return 加密后的用户密码
     */
    @Override
    public String getEncryptedPassword(String password) {
        // 加密时加上盐值
        return DigestUtils.md5DigestAsHex((SALT + password).getBytes());
    }

    /**
     * 将 User 对象转换为 UserVO
     *
     * @param user 用户对象
     * @return 已脱敏的用户信息
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null)
            return null;

        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);

        return userVO;
    }

    /**
     * 获取脱敏后的 UserVO 列表
     *
     * @param userList 用户对象列表
     * @return 已脱敏的用户信息列表
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (userList == null || userList.isEmpty())
            return new ArrayList<>();

        return userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
    }

    /**
     * 将 User 对象转换为 LoginUserVO
     *
     * @param user 用户对象
     * @return 已脱敏的登录用户信息
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null)
            return null;

        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);

        return loginUserVO;
    }

    /**
     * 根据查询用户的请求获取查询条件
     *
     * @param queryRequest
     * @return 查询条件对象
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest queryRequest) {
        // 判空
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR,
                "用户查询请求为空");

        // 获取请求中的参数
        Long id = queryRequest.getId();
        String userName = queryRequest.getUserName();
        String userAccount = queryRequest.getUserAccount();
        String userProfile = queryRequest.getUserProfile();
        String userRole = queryRequest.getUserRole();
        String sortField = queryRequest.getSortField();
        String sortOrder = queryRequest.getSortOrder();

        // 新建 queryWrapper
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        // 对 id 和 userRole 查询
        queryWrapper.eq(ObjectUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        // 对 name account 和 profile 模糊查询
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);

        return queryWrapper;
    }
}


