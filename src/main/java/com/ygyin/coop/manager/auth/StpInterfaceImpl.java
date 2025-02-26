package com.ygyin.coop.manager.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.ygyin.coop.exception.BusinessException;
import com.ygyin.coop.exception.ErrorCode;
import com.ygyin.coop.exception.ThrowUtils;
import com.ygyin.coop.manager.auth.model.AreaUserPermissionConstant;
import com.ygyin.coop.model.entity.Area;
import com.ygyin.coop.model.entity.AreaUser;
import com.ygyin.coop.model.entity.Image;
import com.ygyin.coop.model.entity.User;
import com.ygyin.coop.model.enums.AreaRoleEnum;
import com.ygyin.coop.model.enums.AreaTypeEnum;
import com.ygyin.coop.service.AreaService;
import com.ygyin.coop.service.AreaUserService;
import com.ygyin.coop.service.ImageService;
import com.ygyin.coop.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static com.ygyin.coop.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 自定义权限加载接口实现类
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Resource
    private ImageService imageService;

    @Resource
    private UserService userService;

    @Resource
    private AreaService areaService;

    @Resource
    private AreaUserService areaUserService;

    @Resource
    private AreaUserAuthManager areaUserAuthManager;

    /**
     * 从请求中获取上下文对象
     */
    private AreaUserAuthContext getAuthContextByRequest() {
        // 1. 获得 HttpServletRequest，从中获得 content-type 以判断是 post / get 请求
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());
        AreaUserAuthContext authContext;

        // 2.1 返回类型的为 Json 为 post，取得请求体转换为上下文
        if (ContentType.JSON.getValue().equals(contentType)) {
            // HttpServletRequest 的 body 值为流，只支持读取一次
            String body = ServletUtil.getBody(request);
            authContext = JSONUtil.toBean(body, AreaUserAuthContext.class);
        }
        // 2.2 get 请求获取参数 map，转换为上下文
        else {
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            authContext = BeanUtil.toBean(paramMap, AreaUserAuthContext.class);
        }
        // 根据请求路径区分 id 字段的含义
        Long id = authContext.getId();
        if (id != null) {
            // 请求业务前缀为 /api/image/abc?a=1
            String requestUri = request.getRequestURI();
            // 替换掉 /api 为 ""
            String partUri = requestUri.replace(contextPath + "/", "");
            // 截取第一个 / 前的字符串
            String moduleName = StrUtil.subBefore(partUri, "/", false);
            switch (moduleName) {
                case "image":
                    authContext.setImgId(id);
                    break;
                case "areaUser":
                    authContext.setAreaUserId(id);
                    break;
                case "area":
                    authContext.setAreaId(id);
                    break;
                default:
            }
        }
        return authContext;
    }


    /**
     * 返回一个账号所拥有的权限码集合
     * 通用权限校验逻辑，兼容公共图库、私有空间及团队空间
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 1. 判断 loginType，仅对类型为 "area" 进行权限校验，返回空权限列表达标无权限
        if (!StpKit.AREA_TYPE.equals(loginType))
            return new ArrayList<>();
        // 2. 定义管理员权限，用于后续返回管理员权限列表
        List<String> ADMIN_PERMISSIONS = areaUserAuthManager.getPermissionsByRole(AreaRoleEnum.ADMIN.getVal());

        // 3. 从请求中获取上下文对象
        AreaUserAuthContext authContext = getAuthContextByRequest();
        // 如果所有字段都为空，表示查询公共图库，可以通过
        if (isAllFieldsNull(authContext))
            return ADMIN_PERMISSIONS;

        // 4. 通过 Sa-token 获取当前登录用户，未登录抛异常
        User loginUser = (User) StpKit.AREA.getSessionByLoginId(loginId).get(USER_LOGIN_STATE);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH, "用户未登录");

        Long userId = loginUser.getId();
        // 5. 如果已经登陆，先从上下文中尝试获取 AreaUser 对象，不为空就按照 areaUser 的角色返回权限
        AreaUser areaUser = authContext.getAreaUser();
        if (areaUser != null)
            return areaUserAuthManager.getPermissionsByRole(areaUser.getAreaRole());

        // 6. 如果没有 areaUser 对象，但有 areaUserId，必然是团队空间，通过数据库查询 AreaUser 对象
        Long areaUserId = authContext.getAreaUserId();
        if (areaUserId != null) {
            areaUser = areaUserService.getById(areaUserId);
            ThrowUtils.throwIf(areaUser == null, ErrorCode.NOT_FOUND, "未找到空间成员信息");

            // 取出当前登录用户对应的 areaUser
            AreaUser loginAreaUser = areaUserService.lambdaQuery()
                    .eq(AreaUser::getAreaId, areaUser.getAreaId())
                    .eq(AreaUser::getUserId, userId)
                    .one();
            if (loginAreaUser == null)
                return new ArrayList<>();

            // todo 这里会导致管理员在私有空间没有权限，可以再查一次库处理
            return areaUserAuthManager.getPermissionsByRole(loginAreaUser.getAreaRole());
        }
        // 7. 如果没有 areaUserId，尝试通过 areaId 或 imgId 获取 Area 对象并处理
        Long areaId = authContext.getAreaId();
        if (areaId == null) {
            // 如果没有 areaId，尝试通过 imgId 获取 Image 对象和 areaId
            Long imgId = authContext.getImgId();
            // 如果没有 areaId 也没有 imgId 也没有，则默认通过权限校验
            if (imgId == null)
                return ADMIN_PERMISSIONS;
            // 有 imgId，查询 image 对象
            Image image = imageService.lambdaQuery()
                    .eq(Image::getId, imgId)
                    .select(Image::getId, Image::getAreaId, Image::getUserId)
                    .one();
            ThrowUtils.throwIf(image == null, ErrorCode.NOT_FOUND, "未找到图片信息");

            // 通过 image 对象去获取 areaId 该图片处于哪个空间
            areaId = image.getAreaId();
            // 公共图库，仅本人或管理员可操作
            if (areaId == null) {
                if (image.getUserId().equals(userId) || userService.isAdmin(loginUser))
                    return ADMIN_PERMISSIONS;
                else
                    // 不是自己的图片，仅可查看
                    return Collections.singletonList(AreaUserPermissionConstant.IMAGE_VIEW);
            }
        }
        // 8. areaId 不为空，获取 Area 对象并根据 areaType 判断权限
        Area area = areaService.getById(areaId);
        if (area == null)
            throw new BusinessException(ErrorCode.NOT_FOUND, "未找到空间信息");

        // 8.1 私有空间，仅本人或管理员有权限
        if (area.getAreaType() == AreaTypeEnum.PRIVATE.getVal()) {
            if (area.getUserId().equals(userId) || userService.isAdmin(loginUser))
                return ADMIN_PERMISSIONS;
            else
                return new ArrayList<>();
        }
        // 8.2 团队空间，通过已有的 areaId 和 userId 查询得到 AreaUser 并获取角色和权限
        else {
            areaUser = areaUserService.lambdaQuery()
                    .eq(AreaUser::getAreaId, areaId)
                    .eq(AreaUser::getUserId, userId)
                    .one();
            if (areaUser == null)
                return new ArrayList<>();

            return areaUserAuthManager.getPermissionsByRole(areaUser.getAreaRole());
        }
    }

    private boolean isAllFieldsNull(Object object) {
        if (object == null)
            return true; // 对象本身为空

        // 获取所有字段并判断是否所有字段都为空
        return Arrays.stream(ReflectUtil.getFields(object.getClass()))
                // 获取字段值
                .map(field -> ReflectUtil.getFieldValue(object, field))
                // 检查是否所有字段都为空
                .allMatch(ObjectUtil::isEmpty);
    }


    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 本 list 仅做模拟，实际项目中要根据具体业务逻辑来查询角色
        return new ArrayList<>();
    }
}
