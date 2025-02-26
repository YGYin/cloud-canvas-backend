package com.ygyin.coop.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ygyin.coop.exception.ErrorCode;
import com.ygyin.coop.exception.ThrowUtils;
import com.ygyin.coop.manager.sharding.DynamicShardingManager;
import com.ygyin.coop.model.dto.area.AreaAddRequest;
import com.ygyin.coop.model.dto.area.AreaQueryRequest;
import com.ygyin.coop.model.entity.Area;
import com.ygyin.coop.model.entity.AreaUser;
import com.ygyin.coop.model.entity.User;
import com.ygyin.coop.model.enums.AreaLevelEnum;
import com.ygyin.coop.model.enums.AreaRoleEnum;
import com.ygyin.coop.model.enums.AreaTypeEnum;
import com.ygyin.coop.model.vo.AreaVO;
import com.ygyin.coop.service.AreaService;
import com.ygyin.coop.mapper.AreaMapper;
import com.ygyin.coop.service.AreaUserService;
import com.ygyin.coop.service.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author yg
 * @description 针对表【area(空间)】的数据库操作Service实现
 * @createDate 2025-02-11 11:41:31
 */
@Service
public class AreaServiceImpl extends ServiceImpl<AreaMapper, Area>
        implements AreaService {

    @Resource
    private UserService userService;

    @Resource
    private AreaUserService areaUserService;

    @Resource
    private TransactionTemplate transactionTemplate;

//    @Resource
//    @Lazy
//    private DynamicShardingManager dynamicShardingManager;


    @Override
    public void verifyArea(Area area, boolean isAdd) {
        // 1. 校验对象是否为空，从对象中取值
        ThrowUtils.throwIf(area == null, ErrorCode.PARAMS_ERROR, "Service: 空间对象为空");
        String areaName = area.getAreaName();
        Integer areaLevel = area.getAreaLevel();
        AreaLevelEnum levelEnum = AreaLevelEnum.getEnumByVal(areaLevel);
        Integer areaType = area.getAreaType();
        AreaTypeEnum areaTypeEnum = AreaTypeEnum.getEnumByVal(areaType);
        // 2. 如果为创建新空间请求，需要校验空间名称，等级和类型是否为空
        if (isAdd) {
            ThrowUtils.throwIf(areaLevel == null,
                    ErrorCode.PARAMS_ERROR, "Service: 空间等级不能为空");

            ThrowUtils.throwIf(areaName.isEmpty(),
                    ErrorCode.PARAMS_ERROR, "Service: 空间名称不能为空");

            ThrowUtils.throwIf(areaType == null,
                    ErrorCode.PARAMS_ERROR, "Service: 空间类型不能为空");
        }
        // 3. 为更新空间请求，需要检查名称是否合法，等级是否合法
        ThrowUtils.throwIf(areaName.length() > 25,
                ErrorCode.PARAMS_ERROR, "Service: 空间名称过长，请限制长度在 25 个字符内");
        ThrowUtils.throwIf(areaLevel != null && levelEnum == null,
                ErrorCode.PARAMS_ERROR, "Service: 空间等级不合法");
        ThrowUtils.throwIf(areaType != null && areaTypeEnum == null,
                ErrorCode.PARAMS_ERROR, "Service: 空间类别不合法");
    }

    @Override
    public long addArea(AreaAddRequest areaAddRequest, User loginUser) {
        // 1. 将 request 转为 area 实体类，填充参数默认值
        Area area = new Area();
        BeanUtil.copyProperties(areaAddRequest, area);
        // 请求中的参数如果为空，则补充默认值
        if (areaAddRequest.getAreaName().isEmpty())
            area.setAreaName("Default Area");
        if (areaAddRequest.getAreaLevel() == null)
            area.setAreaLevel(AreaLevelEnum.DEFAULT.getVal());
        if (areaAddRequest.getAreaType() == null)
            area.setAreaType(AreaTypeEnum.PRIVATE.getVal());
        this.setDefaultAreaByLevel(area);

        // 2. 对 area 中的数据进行过校验，并设置 userId
        this.verifyArea(area, true);
        Long userId = loginUser.getId();
        area.setUserId(userId);

        // 3. 权限校验
        ThrowUtils.throwIf(AreaLevelEnum.DEFAULT.getVal() != areaAddRequest.getAreaLevel()
                        && !userService.isAdmin(loginUser),
                ErrorCode.NO_AUTH, "Service: 没有足够权限创建空间");

        // 4. 令同一个用户只能创建一个私有空间和一个团队空间
        //     针对 userId 来进行加锁操作数据库，不同的用户可以拿到不同的锁
        //     避免对整个方法进行加锁
        String lock = String.valueOf(userId).intern();
        synchronized (lock) {
            Long addAreaId = transactionTemplate.execute(status -> {
                // 查询 Area 表中是否存在该用户创建的私有空间或团队空间
                boolean userExists = this.lambdaQuery()
                        .eq(Area::getUserId, userId)
                        .eq(Area::getAreaType, area.getAreaType())
                        .exists();
                ThrowUtils.throwIf(userExists,
                        ErrorCode.OPERATION_ERROR, "Service: 当前用户已创建空间，每个用户只能创建一个私有或团队空间");
                // 操作数据库进行写入
                boolean isSave = this.save(area);
                ThrowUtils.throwIf(!isSave, ErrorCode.OPERATION_ERROR, "Service: 未成功创建空间");

                // 创建空间成功，如果该空间为团队空间，将当前空间用户记录添加到空间成员表
                if (area.getAreaType() == AreaTypeEnum.TEAM.getVal()) {
                    AreaUser areaUser = new AreaUser();
                    areaUser.setAreaId(area.getId());
                    areaUser.setUserId(userId);
                    areaUser.setAreaRole(AreaRoleEnum.ADMIN.getVal());
                    // 保存记录到数据库
                    boolean isAreaUserSave = areaUserService.save(areaUser);
                    ThrowUtils.throwIf(!isAreaUserSave,
                            ErrorCode.OPERATION_ERROR, "Service: 未成功添加空间成员");
                }
                // 如果是 Ultra 团队空间，动态创建分表
//                dynamicShardingManager.createImageSubTableByArea(area);

                return area.getId();
            });
            // 返回包装类
            return Optional.ofNullable(addAreaId).orElse(-1L);
        }
    }

    @Override
    public AreaVO getAreaVO(Area area, HttpServletRequest request) {
        // 此时 areaVO 中的 userVO 应为空
        AreaVO areaVO = AreaVO.objToVo(area);
        // 获取关联的用户信息，通过 area 中的 userId 获取 user 转为 userVO 保存到 areaVO 中
        Long userId = area.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            areaVO.setUser(userService.getUserVO(user));
        }
        return areaVO;
    }

    @Override
    public Page<AreaVO> getAreaVOPage(Page<Area> areaPage, HttpServletRequest request) {
        // 1. 从 areaPage 中获取对象列表，判空
        List<Area> areaList = areaPage.getRecords();
        Page<AreaVO> areaVOPage = new Page<>(
                areaPage.getCurrent(),
                areaPage.getSize(),
                areaPage.getTotal());
        if (areaList.isEmpty())
            return areaVOPage;

        // 2. 将对象列表转换为封装对象列表
        List<AreaVO> areaVOList = areaList.stream()
                .map(AreaVO::objToVo)
                .collect(Collectors.toList());
        // 3. 通过图片列表获取用户 id 列表，再通过用户 id 关联查询用户信息收集为 map
        Set<Long> userIdSet = areaList.stream().map(Area::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> idToUserListMap = userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));
        // 4. 并填充用户信息到封装图片对象列表中
        areaVOList.forEach(areaVO -> {
            Long userId = areaVO.getUserId();
            User user = null;
            if (idToUserListMap.containsKey(userId))
                user = idToUserListMap.get(userId).get(0);

            areaVO.setUser(userService.getUserVO(user));
        });
        areaVOPage.setRecords(areaVOList);
        return areaVOPage;
    }


    @Override
    public QueryWrapper<Area> getQueryWrapper(AreaQueryRequest areaQueryRequest) {
        // 1. 对图片查询请求判空
        ThrowUtils.throwIf(areaQueryRequest == null, ErrorCode.PARAMS_ERROR,
                "空间查询请求为空");

        // 2. 从请求对象中取值
        Long id = areaQueryRequest.getId();
        Long userId = areaQueryRequest.getUserId();
        String areaName = areaQueryRequest.getAreaName();
        Integer areaLevel = areaQueryRequest.getAreaLevel();
        Integer areaType = areaQueryRequest.getAreaType();
        String sortField = areaQueryRequest.getSortField();
        String sortOrder = areaQueryRequest.getSortOrder();

        // 3. 新建 QueryWrapper，
        QueryWrapper<Area> queryWrapper = new QueryWrapper<>();
        // 根据 id 和 用户 id 进行查询，及其他字段进行模糊查询
        queryWrapper.eq(ObjectUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtil.isNotEmpty(userId), "userId", userId);

        queryWrapper.like(StrUtil.isNotBlank(areaName), "areaName", areaName);
        queryWrapper.eq(ObjectUtil.isNotEmpty(areaLevel), "areaLevel", areaLevel);
        queryWrapper.eq(ObjectUtil.isNotEmpty(areaType), "areaType", areaType);

        // 对结果进行排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public void setDefaultAreaByLevel(Area area) {
        // 如果管理员没有设置最大空间和最大文件数量，根据空间等级填充限额
        AreaLevelEnum areaLevelEnum = AreaLevelEnum.getEnumByVal(area.getAreaLevel());
        if (areaLevelEnum != null) {
            long maxSize = areaLevelEnum.getMaxSize();
            if (area.getMaxSize() == null)
                area.setMaxSize(maxSize);

            long maxCount = areaLevelEnum.getMaxNum();
            if (area.getMaxNum() == null)
                area.setMaxNum(maxCount);
        }
    }

    @Override
    public void checkUserAreaAuth(User loginUser, Area area) {
        // 限制仅有本人或者管理员可编辑空间
        ThrowUtils.throwIf(!area.getUserId().equals(loginUser.getId())
                && !userService.isAdmin(loginUser), ErrorCode.NO_AUTH);
    }

}




