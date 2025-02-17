package com.ygyin.coop.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ygyin.coop.annotation.AuthVerify;
import com.ygyin.coop.common.BaseResponse;
import com.ygyin.coop.common.DeleteRequest;
import com.ygyin.coop.common.ResUtils;
import com.ygyin.coop.constant.UserConstant;
import com.ygyin.coop.exception.ErrorCode;
import com.ygyin.coop.exception.ThrowUtils;
import com.ygyin.coop.model.dto.area.*;
import com.ygyin.coop.model.dto.area.analyze.AreaCategoryAnalyzeRequest;
import com.ygyin.coop.model.dto.area.analyze.AreaTagAnalyzeRequest;
import com.ygyin.coop.model.dto.area.analyze.AreaUsageAnalyzeRequest;
import com.ygyin.coop.model.entity.Area;
import com.ygyin.coop.model.entity.User;
import com.ygyin.coop.model.enums.AreaLevelEnum;
import com.ygyin.coop.model.vo.AreaVO;
import com.ygyin.coop.model.vo.area.analyze.AreaCategoryAnalyzeResponse;
import com.ygyin.coop.model.vo.area.analyze.AreaTagAnalyzeResponse;
import com.ygyin.coop.model.vo.area.analyze.AreaUsageAnalyzeResponse;
import com.ygyin.coop.service.AreaAnalyzeService;
import com.ygyin.coop.service.AreaService;
import com.ygyin.coop.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/area/analyze")
@Slf4j
public class AreaAnalyzeController {

    @Resource
    private UserService userService;

    @Resource
    private AreaService areaService;

    @Resource
    private AreaAnalyzeService areaAnalyzeService;


    /**
     * 获取空间的储存用量分析
     *
     * @param usageAnalyzeRequest
     * @param request
     * @return
     */
    @PostMapping("/usage")
    public BaseResponse<AreaUsageAnalyzeResponse> getAreaUsageAnalyze(
            @RequestBody AreaUsageAnalyzeRequest usageAnalyzeRequest,
            HttpServletRequest request) {
        // 判空，获取用户调用接口获得结果
        ThrowUtils.throwIf(usageAnalyzeRequest == null,
                ErrorCode.PARAMS_ERROR, "Controller: 分析空间储存用量请求为空");
        User loginUser = userService.getLoginUser(request);
        AreaUsageAnalyzeResponse response = areaAnalyzeService.getAreaUsageAnalyze(usageAnalyzeRequest, loginUser);
        return ResUtils.success(response);
    }

    /**
     * 获取特定空间的图片分类分析
     *
     * @param categoryAnalyzeRequest
     * @param request
     * @return
     */
    @PostMapping("/category")
    public BaseResponse<List<AreaCategoryAnalyzeResponse>> getAreaCategoryAnalyze(
            @RequestBody AreaCategoryAnalyzeRequest categoryAnalyzeRequest,
            HttpServletRequest request) {
        // 判空，获取用户调用接口获得结果
        ThrowUtils.throwIf(categoryAnalyzeRequest == null,
                ErrorCode.PARAMS_ERROR, "Controller: 分析空间图片分类请求为空");
        User loginUser = userService.getLoginUser(request);
        List<AreaCategoryAnalyzeResponse> responseList = areaAnalyzeService.getAreaCategoryAnalyze(categoryAnalyzeRequest, loginUser);
        return ResUtils.success(responseList);
    }

    /**
     * 获取特定空间的图片 tag 分析
     *
     * @param tagAnalyzeRequest
     * @param request
     * @return
     */
    @PostMapping("/tag")
    public BaseResponse<List<AreaTagAnalyzeResponse>> getAreaTagAnalyze(
            @RequestBody AreaTagAnalyzeRequest tagAnalyzeRequest,
            HttpServletRequest request) {
        // 判空，获取用户调用接口获得结果
        ThrowUtils.throwIf(tagAnalyzeRequest == null,
                ErrorCode.PARAMS_ERROR, "Controller: 分析空间图片 tag 请求为空");
        User loginUser = userService.getLoginUser(request);
        List<AreaTagAnalyzeResponse> responseList = areaAnalyzeService.getAreaTagAnalyze(tagAnalyzeRequest, loginUser);
        return ResUtils.success(responseList);
    }

}
