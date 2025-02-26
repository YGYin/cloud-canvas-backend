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
import com.ygyin.coop.manager.auth.AreaUserAuthManager;
import com.ygyin.coop.model.dto.area.*;
import com.ygyin.coop.model.entity.Area;
import com.ygyin.coop.model.entity.User;
import com.ygyin.coop.model.enums.AreaLevelEnum;
import com.ygyin.coop.model.vo.AreaVO;
import com.ygyin.coop.service.AreaService;
import com.ygyin.coop.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/area")
@Slf4j
public class AreaController {

    @Resource
    private UserService userService;

    @Resource
    private AreaService areaService;

    @Resource
    private AreaUserAuthManager areaUserAuthManager;

    @PostMapping("/add")
    public BaseResponse<Long> addArea(@RequestBody AreaAddRequest areaAddRequest,
                                      HttpServletRequest request) {
        // 请求判空，调用服务
        ThrowUtils.throwIf(areaAddRequest == null,
                ErrorCode.PARAMS_ERROR, "Controller: 创建空间请求为空");

        User loginUser = userService.getLoginUser(request);
        long addAreaId = areaService.addArea(areaAddRequest, loginUser);
        return ResUtils.success(addAreaId);
    }

    /**
     * 删除空间
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteArea(
            @RequestBody DeleteRequest deleteRequest,
            HttpServletRequest request) {
        // 对请求判空
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR, "Controller: 删除空间请求为空");
        // 判断删除的空间是否存在
        Long id = deleteRequest.getId();
        Area areaToDelete = areaService.getById(id);
        ThrowUtils.throwIf(areaToDelete == null,
                ErrorCode.NOT_FOUND, "Controller: 删除的空间不存在");

        // 校验权限，只有上传该空间的本人或者管理员才可以删除
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(!areaToDelete.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser),
                ErrorCode.NO_AUTH, "Controller: 当前用户无权限删除该空间");

        // 数据库删除该空间
        boolean isDelete = areaService.removeById(id);
        ThrowUtils.throwIf(!isDelete, ErrorCode.OPERATION_ERROR, "Controller: 未正确删除该空间");

        return ResUtils.success(true);
    }

    /**
     * 更新空间（管理员）
     */
    @PostMapping("/update")
    @AuthVerify(requiredRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateArea(@RequestBody AreaUpdateRequest areaUpdateRequest,
                                            HttpServletRequest request) {
        // 请求判空
        ThrowUtils.throwIf(areaUpdateRequest == null || areaUpdateRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR, "Controller: 管理员更新空间请求为空");
        // 将 请求 dto 转换为 Area 实体类，tags 需要手动转为 Json String
        Area area = new Area();
        BeanUtil.copyProperties(areaUpdateRequest, area);
        // 根据等级设置默认空间容量和数量限制
        areaService.setDefaultAreaByLevel(area);
        // 空间数据校验，判断更新的空间是否存在
        areaService.verifyArea(area, false);
        Long id = areaUpdateRequest.getId();
        Area areaToUpdate = areaService.getById(id);
        ThrowUtils.throwIf(areaToUpdate == null,
                ErrorCode.NOT_FOUND, "Controller: 更新空间不存在");

        // 数据库更新空间
        boolean isUpdate = areaService.updateById(area);
        ThrowUtils.throwIf(!isUpdate, ErrorCode.OPERATION_ERROR, "Controller: 未正确更新该空间");
        return ResUtils.success(true);
    }

    /**
     * 根据 id 获取空间（管理员）
     */
    @GetMapping("/get")
    @AuthVerify(requiredRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Area> getAreaById(long id, HttpServletRequest request) {
        // 对 id 判空
        ThrowUtils.throwIf(id <= 0,
                ErrorCode.PARAMS_ERROR, "Controller: 获取空间 id 不合法");

        // 数据库查询空间
        Area area = areaService.getById(id);
        ThrowUtils.throwIf(area == null,
                ErrorCode.NOT_FOUND, "Controller: 该空间不存在");
        return ResUtils.success(area);
    }

    /**
     * 根据 id 获取空间封装类
     */
    @GetMapping("/get/vo")
    public BaseResponse<AreaVO> getAreaVOById(long id, HttpServletRequest request) {
        // 对 id 判空
        ThrowUtils.throwIf(id <= 0,
                ErrorCode.PARAMS_ERROR, "Controller: 获取空间 id 不合法");

        // 数据库查询空间
        Area area = areaService.getById(id);
        ThrowUtils.throwIf(area == null,
                ErrorCode.NOT_FOUND, "Controller: 该空间不存在");
        User loginUser = userService.getLoginUser(request);
        List<String> permissionList = areaUserAuthManager.getPermissionList(area, loginUser);
        // 转换为 areaVO，同时补充返回给前端的权限列表
        AreaVO areaVO = areaService.getAreaVO(area, request);
        areaVO.setPermissionList(permissionList);

        return ResUtils.success(areaVO);
    }

    /**
     * 分页获取空间列表（管理员）
     */
    @PostMapping("/list/page")
    @AuthVerify(requiredRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Area>> listAreaByPage(@RequestBody AreaQueryRequest areaQueryRequest) {
        // 获取 current 和 size
        int current = areaQueryRequest.getCurrentPage();
        int pageSize = areaQueryRequest.getPageSize();
        // 分页查询数据库
        Page<Area> areaPage = areaService.page(new Page<Area>(current, pageSize),
                areaService.getQueryWrapper(areaQueryRequest));
        return ResUtils.success(areaPage);
    }

    /**
     * 分页获取空间列表（封装类）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<AreaVO>> listAreaVOByPage(@RequestBody AreaQueryRequest areaQueryRequest,
                                                       HttpServletRequest request) {
        // 获取 current 和 size
        int current = areaQueryRequest.getCurrentPage();
        int pageSize = areaQueryRequest.getPageSize();
        // 对用户分页请求的 pageSize 进行限制，防止其进行爬虫
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "Controller: 非法参数");

        // 分页查询数据库
        Page<Area> areaPage = areaService.page(new Page<Area>(current, pageSize),
                areaService.getQueryWrapper(areaQueryRequest));

        // 转为 VO Page 返回
        return ResUtils.success(areaService.getAreaVOPage(areaPage, request));
    }


    /**
     * 编辑空间（用户）
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editArea(@RequestBody AreaEditRequest areaEditRequest, HttpServletRequest request) {
        // 请求判空
        ThrowUtils.throwIf(areaEditRequest == null || areaEditRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR, "Controller: 编辑空间请求为空");
        // 将 请求 dto 转换为 Area 实体类，并设置编辑时间
        Area area = new Area();
        BeanUtil.copyProperties(areaEditRequest, area);
        areaService.setDefaultAreaByLevel(area);
        area.setEditTime(new Date());
        // 空间数据校验，判断更新的空间是否存在
        areaService.verifyArea(area, false);
        Long id = areaEditRequest.getId();
        Area areaToEdit = areaService.getById(id);
        ThrowUtils.throwIf(areaToEdit == null,
                ErrorCode.NOT_FOUND, "Controller: 编辑空间不存在");

        // 获取登录用户，校验只有本人或管理员才可以对空间进行编辑
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(!areaToEdit.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser),
                ErrorCode.NO_AUTH, "Controller: 当前用户无权限编辑该空间");

        // 数据库更新空间
        boolean isEdit = areaService.updateById(area);
        ThrowUtils.throwIf(!isEdit, ErrorCode.OPERATION_ERROR, "Controller: 未正确编辑该空间");
        return ResUtils.success(true);
    }

    /**
     * 获取权限列表
     */
    @GetMapping("/list/level")
    public BaseResponse<List<AreaLevel>> listAreaLevel() {
        List<AreaLevel> levelList = Arrays.stream(AreaLevelEnum.values())
                .map(areaLevelEnum -> new AreaLevel(
                        areaLevelEnum.getVal(),
                        areaLevelEnum.getText(),
                        areaLevelEnum.getMaxNum(),
                        areaLevelEnum.getMaxSize()
                ))
                .collect(Collectors.toList());

        return ResUtils.success(levelList);
    }
}
