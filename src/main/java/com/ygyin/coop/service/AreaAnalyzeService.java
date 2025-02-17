package com.ygyin.coop.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ygyin.coop.model.dto.area.analyze.AreaAnalyzeRequest;
import com.ygyin.coop.model.dto.area.analyze.AreaCategoryAnalyzeRequest;
import com.ygyin.coop.model.dto.area.analyze.AreaTagAnalyzeRequest;
import com.ygyin.coop.model.dto.area.analyze.AreaUsageAnalyzeRequest;
import com.ygyin.coop.model.entity.Area;
import com.ygyin.coop.model.entity.Image;
import com.ygyin.coop.model.entity.User;
import com.ygyin.coop.model.vo.area.analyze.AreaCategoryAnalyzeResponse;
import com.ygyin.coop.model.vo.area.analyze.AreaTagAnalyzeResponse;
import com.ygyin.coop.model.vo.area.analyze.AreaUsageAnalyzeResponse;

import java.util.List;


/**
 * @author yg
 */
public interface AreaAnalyzeService extends IService<Area> {

    /**
     * 根据分析空间的范围，来检查用户是否有权限
     *
     * @param areaAnalyzeRequest
     * @param loginUser
     */
    void checkAreaAnalyzeAuth(AreaAnalyzeRequest areaAnalyzeRequest, User loginUser);

    /**
     * 根据分析请求的分析范围，封装查询条件
     *
     * @param areaAnalyzeRequest
     * @param queryWrapper
     */
    void setAnalyzeQueryWrapper(AreaAnalyzeRequest areaAnalyzeRequest, QueryWrapper<Image> queryWrapper);

    /**
     * 获取空间的储存用量分析数据
     *
     * @param usageAnalyzeRequest 空间储存用量分析请求
     * @param loginUser           登录用户
     * @return
     */
    AreaUsageAnalyzeResponse getAreaUsageAnalyze(AreaUsageAnalyzeRequest usageAnalyzeRequest, User loginUser);

    /**
     * 获取空间的图片分类分析数据
     *
     * @param categoryAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<AreaCategoryAnalyzeResponse> getAreaCategoryAnalyze(AreaCategoryAnalyzeRequest categoryAnalyzeRequest, User loginUser);


    /**
     * 获取空间的图片 tag 分析数据
     *
     * @param tagAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<AreaTagAnalyzeResponse> getAreaTagAnalyze(AreaTagAnalyzeRequest tagAnalyzeRequest, User loginUser);
}