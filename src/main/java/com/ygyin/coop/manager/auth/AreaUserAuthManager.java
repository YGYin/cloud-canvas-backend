package com.ygyin.coop.manager.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.ygyin.coop.manager.auth.model.AreaUserAuthConfig;
import com.ygyin.coop.manager.auth.model.AreaUserRole;
import com.ygyin.coop.model.entity.Area;
import com.ygyin.coop.model.entity.AreaUser;
import com.ygyin.coop.model.entity.User;
import com.ygyin.coop.model.enums.AreaRoleEnum;
import com.ygyin.coop.model.enums.AreaTypeEnum;
import com.ygyin.coop.service.AreaUserService;
import com.ygyin.coop.service.UserService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

import static com.ygyin.coop.model.enums.AreaTypeEnum.PRIVATE;
import static com.ygyin.coop.model.enums.AreaTypeEnum.TEAM;

/**
 * 空间成员权限管理，用于读取 json 配置
 */
@Component
public class AreaUserAuthManager {

    @Resource
    private AreaUserService areaUserService;

    @Resource
    private UserService userService;

    public static final AreaUserAuthConfig AREA_USER_AUTH_CONFIG;

    static {
        // 读取 json 空间成员权限配置，转为空间成员权限及角色全局配置对象
        String json = ResourceUtil.readUtf8Str("business/areaUserAuthConfig.json");
        AREA_USER_AUTH_CONFIG = JSONUtil.toBean(json, AreaUserAuthConfig.class);
    }

    /**
     * 根据空间成员角色获取权限列表
     */
    public List<String> getPermissionsByRole(String areaUserRole) {
        if (StrUtil.isBlank(areaUserRole))
            return new ArrayList<>();

        // 遍历全局配置中的 roles 找到匹配当前空间成员的角色
        AreaUserRole role = AREA_USER_AUTH_CONFIG.getRoles().stream()
                .filter(r -> areaUserRole.equals(r.getKey()))
                .findFirst()
                .orElse(null);

        if (role == null)
            return new ArrayList<>();

        // 返回当前空间成员角色对应的所有权限
        return role.getPermissions();
    }

    /**
     * 用于给前端返回当前空间下该用户所拥有的权限列表
     *
     * @param area
     * @param loginUser
     * @return
     */
    public List<String> getPermissionList(Area area, User loginUser) {
        if (loginUser == null)
            return new ArrayList<>();

        // 定义管理员权限，用于返回
        List<String> ADMIN_PERMISSIONS = getPermissionsByRole(AreaRoleEnum.ADMIN.getVal());
        // 1. 公共图库，用户为管理员则返回所有权限
        if (area == null) {
            if (userService.isAdmin(loginUser))
                return ADMIN_PERMISSIONS;

            return new ArrayList<>();
        }

        // 2. 获取空间类型，如果空间枚举对象为空说明不合法，返回空
        AreaTypeEnum areaTypeEnum = AreaTypeEnum.getEnumByVal(area.getAreaType());
        if (areaTypeEnum == null)
            return new ArrayList<>();

        // 3. 根据空间枚举获取对应的权限
        switch (areaTypeEnum) {
            case PRIVATE:
                // 3.1 私有空间，仅本人或管理员有所有权限
                if (area.getUserId().equals(loginUser.getId()) || userService.isAdmin(loginUser))
                    return ADMIN_PERMISSIONS;
                else
                    return new ArrayList<>();

            case TEAM:
                // 3.2 团队空间，通过 areaId 和 userId 查询 AreaUser 并获取角色和权限
                AreaUser areaUser = areaUserService.lambdaQuery()
                        .eq(AreaUser::getAreaId, area.getId())
                        .eq(AreaUser::getUserId, loginUser.getId())
                        .one();
                if (areaUser == null)
                    return new ArrayList<>();
                else
                    return getPermissionsByRole(areaUser.getAreaRole());

        }
        return new ArrayList<>();
    }

}
