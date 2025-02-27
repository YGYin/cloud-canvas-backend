package com.ygyin.coop.manager.websocket;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.ygyin.coop.manager.auth.AreaUserAuthManager;
import com.ygyin.coop.manager.auth.model.AreaUserPermissionConstant;
import com.ygyin.coop.model.entity.Area;
import com.ygyin.coop.model.entity.Image;
import com.ygyin.coop.model.entity.User;
import com.ygyin.coop.model.enums.AreaTypeEnum;
import com.ygyin.coop.service.AreaService;
import com.ygyin.coop.service.ImageService;
import com.ygyin.coop.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * WebSocket 建立连接握手拦截器
 */
@Component
@Slf4j
public class WebSocketHsInterceptor implements HandshakeInterceptor {

    @Resource
    private UserService userService;

    @Resource
    private ImageService imageService;

    @Resource
    private AreaService areaService;

    @Resource
    private AreaUserAuthManager areaUserAuthManager;

    /**
     * 用于在 WebSocket 建立连接前先做校验
     * @param request
     * @param response
     * @param wsHandler
     * @param attributes 对 WebSocket Session 设置属性
     * @return 是否建立连接
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            // 1. 获取请求参数，校验是否有 图片 id
            String imgId = servletRequest.getParameter("imgId");
            if (StrUtil.isBlank(imgId)) {
                log.error("WebSocket: 缺少图片 id，拒绝握手");
                return false;
            }

            // 2. 获取登录用户
            User loginUser = userService.getLoginUser(servletRequest);
            if (ObjUtil.isEmpty(loginUser)) {
                log.error("WebSocket: 用户未登录，拒绝握手");
                return false;
            }

            // 3. 检查图片 id 是否合法
            Image image = imageService.getById(imgId);
            if (image == null) {
                log.error("WebSocket: 图片 id 不合法或图片不存在，拒绝握手");
                return false;
            }
            // 4. 校验是否为团队空间，如果 areaId 为空则为公共空间
            Long areaId = image.getAreaId();
            Area area = null;
            if (areaId != null) {
                area = areaService.getById(areaId);
                if (area == null) {
                    log.error("WebSocket: 空间不存在，拒绝握手");
                    return false;
                }
                if (area.getAreaType() != AreaTypeEnum.TEAM.getVal()) {
                    log.info("WebSocket: 不是团队空间，拒绝握手");
                    return false;
                }
            }

            // 5. 校验团队空间用户的权限，已兼容公共空间存在多个管理员的情况
            List<String> permissionList = areaUserAuthManager.getPermissionList(area, loginUser);
            if (!permissionList.contains(AreaUserPermissionConstant.IMAGE_EDIT)) {
                log.error("没有图片编辑权限，拒绝握手");
                return false;
            }
            // 设置 attributes
            attributes.put("user", loginUser);
            attributes.put("userId", loginUser.getId());
            attributes.put("imgId", Long.valueOf(imgId)); // 记得转换为 Long 类型
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
    }
}
