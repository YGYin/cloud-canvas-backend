package com.ygyin.coop.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ygyin.coop.model.dto.area.AreaAddRequest;
import com.ygyin.coop.model.dto.area.AreaQueryRequest;
import com.ygyin.coop.model.entity.Area;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ygyin.coop.model.entity.User;
import com.ygyin.coop.model.vo.AreaVO;

import javax.servlet.http.HttpServletRequest;

/**
 * @author yg
 * @description 针对表【area(空间)】的数据库操作Service
 * @createDate 2025-02-11 11:41:31
 */
public interface AreaService extends IService<Area> {

    /**
     * 新增空间
     *
     * @param areaAddRequest
     * @param loginUser
     * @return 空间 id
     */
    long addArea(AreaAddRequest areaAddRequest, User loginUser);

    /**
     * 获取封装后的空间 VO 对象，并为空间关联创建用户的信息
     *
     * @param area
     * @param request
     * @return
     */
    AreaVO getAreaVO(Area area, HttpServletRequest request);

    /**
     * 分页获取封装后的空间 VO 对象
     *
     * @param areaPage
     * @param request
     * @return
     */
    Page<AreaVO> getAreaVOPage(Page<Area> areaPage, HttpServletRequest request);

    /**
     * 校验空间的参数是否合法
     *
     * @param area
     * @param isAdd 是否为新增空间请求
     */
    void verifyArea(Area area, boolean isAdd);

    /**
     * 根据查询空间的请求构造查询条件
     *
     * @param areaQueryRequest 空间查询请求
     * @return
     */
    QueryWrapper<Area> getQueryWrapper(AreaQueryRequest areaQueryRequest);

    /**
     * 如果管理员未手动设置限额，根据空间等级设置默认最大空间和文件数量
     *
     * @param area
     */
    void setDefaultAreaByLevel(Area area);

    /**
     * 检查当前用户是否有权访问该空间
     *
     * @param loginUser
     * @param area
     */
    void checkUserAreaAuth(User loginUser, Area area);
}
