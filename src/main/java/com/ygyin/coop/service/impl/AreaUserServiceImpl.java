package com.ygyin.coop.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ygyin.coop.exception.ErrorCode;
import com.ygyin.coop.exception.ThrowUtils;
import com.ygyin.coop.model.dto.areauser.AreaUserAddRequest;
import com.ygyin.coop.model.dto.areauser.AreaUserQueryRequest;
import com.ygyin.coop.model.entity.Area;
import com.ygyin.coop.model.entity.AreaUser;
import com.ygyin.coop.model.entity.User;
import com.ygyin.coop.model.enums.AreaRoleEnum;
import com.ygyin.coop.model.vo.AreaUserVO;
import com.ygyin.coop.model.vo.AreaVO;
import com.ygyin.coop.model.vo.UserVO;
import com.ygyin.coop.service.AreaService;
import com.ygyin.coop.service.AreaUserService;
import com.ygyin.coop.mapper.AreaUserMapper;
import com.ygyin.coop.service.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author yg
 * @description 针对表【area_user(空间用户关联)】的数据库操作Service实现
 * @createDate 2025-02-23 15:29:54
 */
@Service
public class AreaUserServiceImpl extends ServiceImpl<AreaUserMapper, AreaUser>
        implements AreaUserService {

    @Resource
    private UserService userService;

    @Resource
    @Lazy
    private AreaService areaService;

    @Override
    public long addAreaUser(AreaUserAddRequest areaUserAddRequest) {
        // 校验参数
        ThrowUtils.throwIf(areaUserAddRequest == null,
                ErrorCode.PARAMS_ERROR, "Service: 创建空间成员请求为空");
        // 构造对象
        AreaUser areaUser = new AreaUser();
        BeanUtil.copyProperties(areaUserAddRequest, areaUser);
        verifyAreaUser(areaUser, true);

        // 存入数据库
        boolean isSave = this.save(areaUser);
        ThrowUtils.throwIf(!isSave, ErrorCode.OPERATION_ERROR, "Service: 创建空间成员失败");
        return areaUser.getId();
    }

    @Override
    public void verifyAreaUser(AreaUser areaUser, boolean isAdd) {
        ThrowUtils.throwIf(areaUser == null,
                ErrorCode.PARAMS_ERROR, "Service: 空间成员为空");

        // 1. 在创建空间成员时，必须有空间和用户 id
        Long areaId = areaUser.getAreaId();
        Long userId = areaUser.getUserId();
        if (isAdd) {
            ThrowUtils.throwIf(ObjectUtil.hasEmpty(areaId, userId),
                    ErrorCode.PARAMS_ERROR, "Service: 用户或空间不能为空");

            User user = userService.getById(userId);
            ThrowUtils.throwIf(user == null,
                    ErrorCode.NOT_FOUND, "Service: 该用户不存在");

            Area area = areaService.getById(areaId);
            ThrowUtils.throwIf(area == null,
                    ErrorCode.NOT_FOUND, "Service: 该空间不存在");
        }

        // 2. 获取该空间成员的角色
        String areaRole = areaUser.getAreaRole();
        AreaRoleEnum areaRoleEnum = AreaRoleEnum.getEnumByVal(areaRole);

        ThrowUtils.throwIf(areaRole != null && areaRoleEnum == null,
                ErrorCode.PARAMS_ERROR, "Service: 空间角色不存在或不合法");
    }


    @Override
    public QueryWrapper<AreaUser> getQueryWrapper(AreaUserQueryRequest areaUserQueryRequest) {
        // 检查查询请求是否为空
        QueryWrapper<AreaUser> queryWrapper = new QueryWrapper<>();
        if (areaUserQueryRequest == null)
            return queryWrapper;

        // 2. 从对象中取值，构造查询条件
        Long id = areaUserQueryRequest.getId();
        Long areaId = areaUserQueryRequest.getAreaId();
        Long userId = areaUserQueryRequest.getUserId();
        String areaRole = areaUserQueryRequest.getAreaRole();

        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(areaId), "areaId", areaId);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(areaRole), "areaRole", areaRole);
        return queryWrapper;
    }


    @Override
    public AreaUserVO getAreaUserVO(AreaUser areaUser, HttpServletRequest request) {
        // 1. 对象转封装类
        AreaUserVO areaUserVO = AreaUserVO.objToVo(areaUser);
        // 2. 通过 userId 关联查询用户信息，补充 userVO
        Long userId = areaUser.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            areaUserVO.setUser(userVO);
        }
        // 3. 通过 areaId 关联查询空间信息，补充 areaVO
        Long areaId = areaUser.getAreaId();
        if (areaId != null && areaId > 0) {
            Area area = areaService.getById(areaId);
            AreaVO areaVO = areaService.getAreaVO(area, request);
            areaUserVO.setArea(areaVO);
        }
        return areaUserVO;
    }

    @Override
    public List<AreaUserVO> getAreaUserVOList(List<AreaUser> areaUserList) {
        // 1. 判断空间成员列表是否为空
        if (CollUtil.isEmpty(areaUserList))
            return Collections.emptyList();

        // 2. 将空间成员对象列表转换为空间成员 VO 列表
        List<AreaUserVO> areaUserVOList = areaUserList.stream()
                .map(AreaUserVO::objToVo)
                .collect(Collectors.toList());

        // 3.1 通过空间成员列表获取用户 id 和空间 id 列表
        Set<Long> userIdSet = areaUserList.stream().map(AreaUser::getUserId).collect(Collectors.toSet());
        Set<Long> areaIdSet = areaUserList.stream().map(AreaUser::getAreaId).collect(Collectors.toSet());
        // 3.2 再用用户 id 和空间 id 列表，关联查询用户信息和空间信息收集成 map
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        Map<Long, List<Area>> areaIdAreaListMap = areaService.listByIds(areaIdSet).stream()
                .collect(Collectors.groupingBy(Area::getId));

        // 4. 填充 AreaUserVO 的用户和空间信息
        areaUserVOList.forEach(areaUserVO -> {
            Long userId = areaUserVO.getUserId();
            Long areaId = areaUserVO.getAreaId();
            // 填充用户信息
            User user = null;
            if (userIdUserListMap.containsKey(userId))
                user = userIdUserListMap.get(userId).get(0);
            areaUserVO.setUser(userService.getUserVO(user));

            // 填充空间信息
            Area area = null;
            if (areaIdAreaListMap.containsKey(areaId))
                area = areaIdAreaListMap.get(areaId).get(0);
            areaUserVO.setArea(AreaVO.objToVo(area));
        });

        return areaUserVOList;
    }

}




