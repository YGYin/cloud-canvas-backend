package com.ygyin.coop.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ygyin.coop.common.BaseResponse;
import com.ygyin.coop.common.DeleteRequest;
import com.ygyin.coop.common.ResUtils;
import com.ygyin.coop.exception.ErrorCode;
import com.ygyin.coop.exception.ThrowUtils;
import com.ygyin.coop.manager.auth.annotation.SaAreaCheckPermission;
import com.ygyin.coop.manager.auth.model.AreaUserPermissionConstant;
import com.ygyin.coop.model.dto.areauser.AreaUserAddRequest;
import com.ygyin.coop.model.dto.areauser.AreaUserEditRequest;
import com.ygyin.coop.model.dto.areauser.AreaUserQueryRequest;
import com.ygyin.coop.model.entity.AreaUser;
import com.ygyin.coop.model.entity.User;
import com.ygyin.coop.model.vo.AreaUserVO;
import com.ygyin.coop.service.AreaUserService;
import com.ygyin.coop.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/areaUser")
@Slf4j
public class AreaUserController {

    @Resource
    private UserService userService;

    @Resource
    private AreaUserService areaUserService;

    /**
     * 添加用户为空间成员
     *
     * @param areaUserAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    @SaAreaCheckPermission(value = AreaUserPermissionConstant.AREA_USER_MANAGE)
    public BaseResponse<Long> addAreaUser(@RequestBody AreaUserAddRequest areaUserAddRequest,
                                          HttpServletRequest request) {
        // 请求判空，调用服务
        ThrowUtils.throwIf(areaUserAddRequest == null,
                ErrorCode.PARAMS_ERROR, "Controller: 创建空间成员请求为空");

        long addAreaUserId = areaUserService.addAreaUser(areaUserAddRequest);
        return ResUtils.success(addAreaUserId);
    }

    /**
     * 删除空间成员
     */
    @PostMapping("/delete")
    @SaAreaCheckPermission(value = AreaUserPermissionConstant.AREA_USER_MANAGE)
    public BaseResponse<Boolean> deleteAreaUser(
            @RequestBody DeleteRequest deleteRequest,
            HttpServletRequest request) {
        // 对请求判空
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR, "Controller: 删除空间成员请求为空");

        // 判断删除的空间成员是否存在
        Long id = deleteRequest.getId();
        AreaUser areaUserToDelete = areaUserService.getById(id);
        ThrowUtils.throwIf(areaUserToDelete == null,
                ErrorCode.NOT_FOUND, "Controller: 删除的空间成员不存在");

        // 数据库删除该空间成员
        boolean isDelete = areaUserService.removeById(id);
        ThrowUtils.throwIf(!isDelete, ErrorCode.OPERATION_ERROR, "Controller: 未正确删除该空间成员");

        return ResUtils.success(true);
    }


    /**
     * 根据 id 获取空间（管理员）
     */
    @GetMapping("/get")
    @SaAreaCheckPermission(value = AreaUserPermissionConstant.AREA_USER_MANAGE)
    public BaseResponse<AreaUser> getAreaUser(@RequestBody AreaUserQueryRequest areaUserQueryRequest) {
        // 对请求判空
        ThrowUtils.throwIf(areaUserQueryRequest == null,
                ErrorCode.PARAMS_ERROR, "Controller: 查询空间成员请求为空");

        Long areaId = areaUserQueryRequest.getAreaId();
        Long userId = areaUserQueryRequest.getUserId();
        ThrowUtils.throwIf(ObjectUtil.hasEmpty(areaId, userId),
                ErrorCode.PARAMS_ERROR, "Controller: 该空间成员或空间 id 不合法");

        // 数据库查询空间成员
        QueryWrapper<AreaUser> queryWrapper = areaUserService.getQueryWrapper(areaUserQueryRequest);
        AreaUser areaUser = areaUserService.getOne(queryWrapper);
        ThrowUtils.throwIf(areaUser == null,
                ErrorCode.NOT_FOUND, "Controller: 该空间用户不存在");

        return ResUtils.success(areaUser);
    }

    /**
     * 查询空间成员信息列表
     */
    @PostMapping("/list")
    @SaAreaCheckPermission(value = AreaUserPermissionConstant.AREA_USER_MANAGE)
    public BaseResponse<List<AreaUserVO>> listAreaUser(@RequestBody AreaUserQueryRequest areaUserQueryRequest,
                                                       HttpServletRequest request) {
        ThrowUtils.throwIf(areaUserQueryRequest == null,
                ErrorCode.PARAMS_ERROR, "Controller: 查询空间成员请求为空");

        QueryWrapper<AreaUser> queryWrapper = areaUserService.getQueryWrapper(areaUserQueryRequest);
        List<AreaUser> areaUserList = areaUserService.list(queryWrapper);

        return ResUtils.success(areaUserService.getAreaUserVOList(areaUserList));
    }


    /**
     * 编辑空间成员信息（设置权限）
     */
    @PostMapping("/edit")
    @SaAreaCheckPermission(value = AreaUserPermissionConstant.AREA_USER_MANAGE)
    public BaseResponse<Boolean> editAreaUser(@RequestBody AreaUserEditRequest areaUserEditRequest,
                                              HttpServletRequest request) {

        ThrowUtils.throwIf(areaUserEditRequest == null || areaUserEditRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR, "Controller: 该空间成员不存在或非法");

        // 将编辑请求转换为空间用户实体类
        AreaUser areaUser = new AreaUser();
        BeanUtils.copyProperties(areaUserEditRequest, areaUser);
        // 数据校验
        areaUserService.verifyAreaUser(areaUser, false);

        // 判断空间用户是否存在
        long id = areaUserEditRequest.getId();
        AreaUser areaUserToEdit = areaUserService.getById(id);
        ThrowUtils.throwIf(areaUserToEdit == null,
                ErrorCode.NOT_FOUND, "Controller: 该空间成员不存在");

        // 操作数据库
        boolean isUpdate = areaUserService.updateById(areaUser);
        ThrowUtils.throwIf(!isUpdate,
                ErrorCode.OPERATION_ERROR, "Controller: 未成功编辑该空间用户的权限");

        return ResUtils.success(true);
    }

    /**
     * 查询当前用户加入的团队空间列表
     */
    @PostMapping("/list/my")
    public BaseResponse<List<AreaUserVO>> listMyTeamArea(HttpServletRequest request) {
        // 1. 获取登录用户，构造空间成员查询请求
        User loginUser = userService.getLoginUser(request);
        AreaUserQueryRequest areaUserQueryRequest = new AreaUserQueryRequest();
        areaUserQueryRequest.setUserId(loginUser.getId());
        // 2. 查询该用户的用户列表
        QueryWrapper<AreaUser> queryWrapper = areaUserService.getQueryWrapper(areaUserQueryRequest);
        List<AreaUser> areaUserList = areaUserService.list(queryWrapper);

        return ResUtils.success(areaUserService.getAreaUserVOList(areaUserList));
    }
}
