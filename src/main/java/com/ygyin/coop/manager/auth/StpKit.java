package com.ygyin.coop.manager.auth;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

/**
 * StpLogic 门面类，用于管理项目中不同的 StpLogic 账号体系
 * 目前用于团队空间中的成员权限校验
 */
@Component
public class StpKit {

    public static final String AREA_TYPE = "area";

    /**
     * 默认原生会话对象，项目中暂未使用
     */
    public static final StpLogic DEFAULT = StpUtil.stpLogic;

    /**
     * Area 会话对象，管理 Area 表所有账号的登录、权限认证
     */
    public static final StpLogic AREA = new StpLogic(AREA_TYPE);
}

