package com.ygyin.coop.service.impl;

import cn.hutool.core.util.NumberUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ygyin.coop.exception.BusinessException;
import com.ygyin.coop.exception.ErrorCode;
import com.ygyin.coop.exception.ThrowUtils;
import com.ygyin.coop.mapper.AreaMapper;
import com.ygyin.coop.model.dto.area.analyze.AreaAnalyzeRequest;
import com.ygyin.coop.model.dto.area.analyze.AreaCategoryAnalyzeRequest;
import com.ygyin.coop.model.dto.area.analyze.AreaUsageAnalyzeRequest;
import com.ygyin.coop.model.entity.Area;
import com.ygyin.coop.model.entity.Image;
import com.ygyin.coop.model.entity.User;
import com.ygyin.coop.model.vo.area.analyze.AreaCategoryAnalyzeResponse;
import com.ygyin.coop.model.vo.area.analyze.AreaUsageAnalyzeResponse;
import com.ygyin.coop.service.AreaAnalyzeService;
import com.ygyin.coop.service.AreaService;
import com.ygyin.coop.service.ImageService;
import com.ygyin.coop.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author yg
 */
@Service
public class AreaAnalyzeServiceImpl extends ServiceImpl<AreaMapper, Area>
        implements AreaAnalyzeService {

    @Resource
    private UserService userService;

    @Resource
    private AreaService areaService;

    @Resource
    private ImageService imageService;


    /**
     * 根据分析空间的范围，来检查用户是否有权限
     *
     * @param areaAnalyzeRequest
     * @param loginUser
     */
    @Override
    public void checkAreaAnalyzeAuth(AreaAnalyzeRequest areaAnalyzeRequest, User loginUser) {
        // 根据分析空间的范围来检查权限
        // 1. 如果分析全部空间或者分析公共空间，用户必须是管理员
        if (areaAnalyzeRequest.isAnalyzeAll() || areaAnalyzeRequest.isAnalyzePublic())
            ThrowUtils.throwIf(!userService.isAdmin(loginUser),
                    ErrorCode.NO_AUTH, "Service: 无权访问公共空间分析数据");
            // 如果分析个人空间，则通过请求获取到当前空间，检查用户是否为空间所有者或管理员
        else {
            Long areaId = areaAnalyzeRequest.getAreaId();
            ThrowUtils.throwIf(areaId == null || areaId <= 0,
                    ErrorCode.PARAMS_ERROR, "Service: 当前用户非法");
            Area area = areaService.getById(areaId);
            ThrowUtils.throwIf(area == null,
                    ErrorCode.NOT_FOUND, "Service: 访问的空间分析数据不存在");
            // 校验是否为空间所有者或管理员
            areaService.checkUserAreaAuth(loginUser, area);
        }
    }

    /**
     * 根据分析请求的分析范围，封装查询条件
     *
     * @param areaAnalyzeRequest
     * @param queryWrapper
     */
    @Override
    public void setAnalyzeQueryWrapper(AreaAnalyzeRequest areaAnalyzeRequest, QueryWrapper<Image> queryWrapper) {
        // 分析全部空间图片，不需要加查询条件
        if (areaAnalyzeRequest.isAnalyzeAll())
            return;
        // 分析公共空间
        if (areaAnalyzeRequest.isAnalyzePublic()) {
            queryWrapper.isNull("areaId");
            return;
        }
        // 分析个人空间
        Long areaId = areaAnalyzeRequest.getAreaId();
        if (areaId != null) {
            queryWrapper.eq("areaId", areaId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "Service: 未指定查询范围");
    }

    @Override
    public AreaUsageAnalyzeResponse getAreaUsageAnalyze(AreaUsageAnalyzeRequest usageAnalyzeRequest, User loginUser) {
        // 1. 校验分析请求参数
        ThrowUtils.throwIf(usageAnalyzeRequest == null,
                ErrorCode.PARAMS_ERROR, "Service: 空间用量查询请求为空");
        // 2.1 如果需要分析全部空间或者公共图库，需要查 image 表累加全部用量
        if (usageAnalyzeRequest.isAnalyzeAll() || usageAnalyzeRequest.isAnalyzePublic()) {
            // 根据空间类型校验用户是否有权限校验空间
            checkAreaAnalyzeAuth(usageAnalyzeRequest, loginUser);
            // 构建查询请求，只查 imgSize 并进行加和
            QueryWrapper<Image> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("imgSize");
            // 根据分析请求中的空间分析范围来补充查询
            this.setAnalyzeQueryWrapper(usageAnalyzeRequest, queryWrapper);
            // 避免使用 .list() 查询，会返回多个 image 对象，应只需要 imgSize 的值
            List<Object> imgSizeList = imageService.getBaseMapper().selectObjs(queryWrapper);
            long usedSize = imgSizeList.stream().mapToLong(obj -> (Long) obj).sum();
            long usedNum = imgSizeList.size();

            // 封装响应类
            AreaUsageAnalyzeResponse usageAnalyzeResponse = new AreaUsageAnalyzeResponse();
            usageAnalyzeResponse.setUsedSize(usedSize);
            usageAnalyzeResponse.setMaxSize(null);
            usageAnalyzeResponse.setUsedSizePercent(null);
            usageAnalyzeResponse.setUsedNum(usedNum);
            usageAnalyzeResponse.setMaxNum(null);
            usageAnalyzeResponse.setUsedNumPercent(null);
            return usageAnalyzeResponse;
        }
        // 2.2 如果查特定空间的储存用量，需要查 area 表
        else {
            // 获取 areaId，判空，获取空间进行判空
            Long areaId = usageAnalyzeRequest.getAreaId();
            ThrowUtils.throwIf(areaId == null || areaId <= 0,
                    ErrorCode.PARAMS_ERROR, "Service: 该空间 id 不合法");
            Area area = areaService.getById(areaId);
            ThrowUtils.throwIf(area == null,
                    ErrorCode.NOT_FOUND, "Service: 该空间不存在");
            // 因为需要 area 对象
            // 不能只利用 checkAreaAnalyzeAuth 自动根据分析范围检验用户是否有权限
            checkAreaAnalyzeAuth(usageAnalyzeRequest, loginUser);

            // 封装响应类
            AreaUsageAnalyzeResponse usageAnalyzeResponse = new AreaUsageAnalyzeResponse();
            usageAnalyzeResponse.setUsedSize(area.getTotalSize());
            usageAnalyzeResponse.setMaxSize(area.getMaxSize());
            double usedSizePercent = NumberUtil.round(area.getTotalSize() * 100.0 / area.getMaxSize(), 2)
                    .doubleValue();
            usageAnalyzeResponse.setUsedSizePercent(usedSizePercent);

            usageAnalyzeResponse.setUsedNum(area.getTotalNum());
            usageAnalyzeResponse.setMaxNum(area.getMaxNum());
            double usedNumPercent = NumberUtil.round(area.getTotalNum() * 100.0 / area.getMaxNum(), 2)
                    .doubleValue();
            usageAnalyzeResponse.setUsedNumPercent(usedNumPercent);

            return usageAnalyzeResponse;
        }
    }

    @Override
    public List<AreaCategoryAnalyzeResponse> getAreaCategoryAnalyze(AreaCategoryAnalyzeRequest categoryAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(categoryAnalyzeRequest == null,
                ErrorCode.PARAMS_ERROR, "Service: 分类分析请求为空");

        // 1. 根据分析范围检验用户是否有权限
        this.checkAreaAnalyzeAuth(categoryAnalyzeRequest, loginUser);

        // 2. 构建查询条件
        QueryWrapper<Image> queryWrapper = new QueryWrapper<>();
        this.setAnalyzeQueryWrapper(categoryAnalyzeRequest, queryWrapper);

        queryWrapper.select("category", "count(*) as totalNum", "sum(imgSize) as totalSize")
                .groupBy("category");

        // 返回多个列，使用 selectMap
        List<AreaCategoryAnalyzeResponse> responseList = imageService.getBaseMapper().selectMaps(queryWrapper)
                .stream()
                .map(res -> {
                    String category = (String) res.get("category");
                    Long totalNum = (Long) res.get("totalNum");
                    Long totalSize = (Long) res.get("totalSize");
                    return new AreaCategoryAnalyzeResponse(category, totalSize, totalNum);
                }).collect(Collectors.toList());

        return responseList;
    }

}
