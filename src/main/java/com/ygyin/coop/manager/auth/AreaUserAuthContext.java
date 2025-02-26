package com.ygyin.coop.manager.auth;

import com.ygyin.coop.model.entity.Area;
import com.ygyin.coop.model.entity.AreaUser;
import com.ygyin.coop.model.entity.Image;
import lombok.Data;

/**
 * 用户在特定空间内的授权上下文，用于统一存储要从请求参数中拿到的值
 */
@Data
public class AreaUserAuthContext {
    /**
     * 临时参数，不同请求对应的 id 可能不同
     */
    private Long id;

    /**
     * 图片 id
     */
    private Long imgId;

    /**
     * 空间 id
     */
    private Long areaId;

    /**
     * 空间成员 id
     */
    private Long areaUserId;

    /**
     * 图片信息
     */
    private Image image;

    /**
     * 空间信息
     */
    private Area area;

    /**
     * 空间用户信息
     */
    private AreaUser areaUser;
}