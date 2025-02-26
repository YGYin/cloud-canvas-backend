package com.ygyin.coop.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ygyin.coop.model.dto.areauser.AreaUserAddRequest;
import com.ygyin.coop.model.dto.areauser.AreaUserQueryRequest;
import com.ygyin.coop.model.entity.AreaUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ygyin.coop.model.vo.AreaUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author yg
* @description 针对表【area_user(空间用户关联)】的数据库操作Service
* @createDate 2025-02-23 15:29:54
*/
public interface AreaUserService extends IService<AreaUser> {

    /**
     * 创建空间成员
     *
     * @param areaUserAddRequest
     * @return
     */
    long addAreaUser(AreaUserAddRequest areaUserAddRequest);

    /**
     * 校验空间成员
     *
     * @param areaUser
     * @param isAdd
     */
    void verifyAreaUser(AreaUser areaUser, boolean isAdd);

    /**
     * 构造空间成员的查询条件
     *
     * @param areaUserQueryRequest
     * @return
     */
    QueryWrapper<AreaUser> getQueryWrapper(AreaUserQueryRequest areaUserQueryRequest);

    /**
     * 获取空间成员 VO 封装类
     *
     * @param areaUser
     * @param request
     * @return
     */
    AreaUserVO getAreaUserVO(AreaUser areaUser, HttpServletRequest request);

    /**
     * 获取空间用户 VO 列表
     *
     * @param areaUserList
     * @return
     */
    List<AreaUserVO> getAreaUserVOList(List<AreaUser> areaUserList);
}
