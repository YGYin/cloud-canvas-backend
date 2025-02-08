package com.ygyin.coop.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ygyin.coop.exception.ErrorCode;
import com.ygyin.coop.exception.ThrowUtils;
import com.ygyin.coop.manager.FileManager;
import com.ygyin.coop.manager.upload.FileImageUpload;
import com.ygyin.coop.manager.upload.ImageUploadTemplate;
import com.ygyin.coop.manager.upload.UrlImageUpload;
import com.ygyin.coop.model.dto.file.UploadImageResult;
import com.ygyin.coop.model.dto.image.ImageQueryRequest;
import com.ygyin.coop.model.dto.image.ImageReviewRequest;
import com.ygyin.coop.model.dto.image.ImageUploadRequest;
import com.ygyin.coop.model.entity.Image;
import com.ygyin.coop.model.entity.User;
import com.ygyin.coop.model.enums.ImageReviewStatusEnum;
import com.ygyin.coop.model.vo.ImageVO;
import com.ygyin.coop.service.ImageService;
import com.ygyin.coop.mapper.ImageMapper;
import com.ygyin.coop.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author yg
 * @description 针对表【image(图片)】的数据库操作Service实现
 * @createDate 2025-01-29 17:07:54
 */
@Service
public class ImageServiceImpl extends ServiceImpl<ImageMapper, Image>
        implements ImageService {

    @Resource
    private FileImageUpload fileImageUpload;

    @Resource
    private UrlImageUpload urlImageUpload;

    @Resource
    private UserService userService;

    /**
     * 上传图片
     *
     * @param inputSource        输入源文件(图片文件 / url)
     * @param imageUploadRequest 图片上传请求封装类
     * @param loginUser          用户
     * @return
     */
    @Override
    public ImageVO uploadImage(Object inputSource, ImageUploadRequest imageUploadRequest, User loginUser) {
        // 1. 校验用户是否为空
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH);
        // 2. 判断是新增还是更新，如果是更新需要根据 imageId 判断图片是否为空
        Long imgId = null;
        if (imageUploadRequest != null)
            imgId = imageUploadRequest.getId();

        // 如果是更新图片，需要校验图片是否存在，及是否为本人或管理员编辑图片
        if (imgId != null) {
            Image oldImage = this.getById(imgId);
            ThrowUtils.throwIf(oldImage == null,
                    ErrorCode.NOT_FOUND, "Service: 原图片不存在");
            // 仅本人或管理员可编辑
            ThrowUtils.throwIf(!oldImage.getUserId().equals(loginUser.getId())
                            && !userService.isAdmin(loginUser),
                    ErrorCode.NO_AUTH, "Service: 仅本人或管理员可编辑原图片");
        }

        // 3. 上传图片，先根据用户 id 或名称构造上传后的路径，通过返回的上传结果得到图片信息
        // todo: 可改为名字
        String pathPrefix = String.format("public/%s", loginUser.getId());

        // 根据上传文件的类型，来区分上传文件的方式
        // 默认为文件类型，但如果上传的为 String 类型，说明为 url
        ImageUploadTemplate imageUploadTemplate = fileImageUpload;
        if (inputSource instanceof String)
            imageUploadTemplate = urlImageUpload;
        // 上传图片文件或 url
        UploadImageResult uploadImgResult = imageUploadTemplate.uploadImage(inputSource, pathPrefix);

        // 4. 根据图片信息构造 ImageVO
        Image image = new Image();

        image.setUrl(uploadImgResult.getUrl());
        image.setName(uploadImgResult.getName());
        image.setImgSize(uploadImgResult.getImgSize());
        image.setImgWidth(uploadImgResult.getImgWidth());
        image.setImgHeight(uploadImgResult.getImgHeight());
        image.setImgScale(uploadImgResult.getImgScale());
        image.setImgFormat(uploadImgResult.getImgFormat());
        image.setUserId(loginUser.getId());
        // 添加审核参数
        this.addReviewParams(image, loginUser);

        // 如果 imageId 不为空，表示为更新，否则为新增图片
        if (imgId != null) {
            image.setId(imgId);
            image.setEditTime(new Date());
        }

        // 5. 保存或更新数据库，返回 ImageVO
        boolean isSaveOrUpdate = this.saveOrUpdate(image);
        ThrowUtils.throwIf(!isSaveOrUpdate, ErrorCode.OPERATION_ERROR, "当前图片上传失败");
        return ImageVO.objToVo(image);
    }

    @Override
    public QueryWrapper<Image> getQueryWrapper(ImageQueryRequest imageQueryRequest) {
        // 1. 对图片查询请求判空
        ThrowUtils.throwIf(imageQueryRequest == null, ErrorCode.PARAMS_ERROR,
                "图片查询请求为空");

        // 2. 从请求对象中取值
        Long id = imageQueryRequest.getId();
        String name = imageQueryRequest.getName();
        String intro = imageQueryRequest.getIntro();
        String category = imageQueryRequest.getCategory();
        List<String> tags = imageQueryRequest.getTags();
        Long imgSize = imageQueryRequest.getImgSize();
        Integer imgWidth = imageQueryRequest.getImgWidth();
        Integer imgHeight = imageQueryRequest.getImgHeight();
        Double imgScale = imageQueryRequest.getImgScale();
        String imgFormat = imageQueryRequest.getImgFormat();
        String searchText = imageQueryRequest.getSearchText();
        Long userId = imageQueryRequest.getUserId();
        String sortField = imageQueryRequest.getSortField();
        String sortOrder = imageQueryRequest.getSortOrder();
        // 获取图片审核相关属性
        Integer reviewStatus = imageQueryRequest.getReviewStatus();
        String reviewMsg = imageQueryRequest.getReviewMsg();
        Long reviewerId = imageQueryRequest.getReviewerId();

        // 3. 新建 QueryWrapper，根据 searchText 搜索图片名称和简介
        QueryWrapper<Image> queryWrapper = new QueryWrapper<>();

        if (StrUtil.isNotBlank(searchText))
            queryWrapper.and(wrapper -> wrapper.like("name", searchText)
                    .or()
                    .like("intro", searchText));

        // 根据 id 和 用户 id 进行查询，及其他字段进行模糊查询
        queryWrapper.eq(ObjectUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtil.isNotEmpty(userId), "userId", userId);

        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(intro), "intro", intro);
        queryWrapper.like(StrUtil.isNotBlank(imgFormat), "imgFormat", imgFormat);

        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjectUtil.isNotEmpty(imgWidth), "imgWidth", imgWidth);
        queryWrapper.eq(ObjectUtil.isNotEmpty(imgHeight), "imgHeight", imgHeight);
        queryWrapper.eq(ObjectUtil.isNotEmpty(imgSize), "imgSize", imgSize);
        queryWrapper.eq(ObjectUtil.isNotEmpty(imgScale), "imgScale", imgScale);

        queryWrapper.eq(ObjectUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMsg), "reviewMsg", reviewMsg);
        queryWrapper.eq(ObjectUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        // tags 使用 Json 数组查询
        if (CollUtil.isNotEmpty(tags))
            for (String tag : tags)
                queryWrapper.like("tags", "\"" + tag + "\"");

        // 对结果进行排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public ImageVO getImageVO(Image image, HttpServletRequest request) {
        // 此时 imageVO 中的 userVO 应为空
        ImageVO imageVO = ImageVO.objToVo(image);
        // 获取关联的用户信息，通过 image 中的 userId 获取 user 转为 userVO 保存到 imageVO 中
        Long userId = image.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            imageVO.setUserVO(userService.getUserVO(user));
        }
        return imageVO;
    }

    @Override
    public Page<ImageVO> getImageVOPage(Page<Image> imagePage, HttpServletRequest request) {
        // 1. 从 imagePage 中获取对象列表，判空
        List<Image> imageList = imagePage.getRecords();
        Page<ImageVO> imageVOPage = new Page<>(
                imagePage.getCurrent(),
                imagePage.getSize(),
                imagePage.getTotal());
        if (imageList.isEmpty())
            return imageVOPage;

        // 2. 将对象列表转换为封装对象列表
        List<ImageVO> imageVOList = imageList.stream()
                .map(ImageVO::objToVo)
                .collect(Collectors.toList());
        // 3. 通过图片列表获取用户 id 列表，再通过用户 id 关联查询用户信息收集为 map
        Set<Long> userIdSet = imageList.stream().map(Image::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> idToUserListMap = userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));
        // 4. 并填充用户信息到封装图片对象列表中
        imageVOList.forEach(imageVO -> {
            Long userId = imageVO.getUserId();
            User user = null;
            if (idToUserListMap.containsKey(userId))
                user = idToUserListMap.get(userId).get(0);

            imageVO.setUserVO(userService.getUserVO(user));
        });
        imageVOPage.setRecords(imageVOList);
        return imageVOPage;
    }

    @Override
    public void verifyImage(Image image) {
        // 判空
        ThrowUtils.throwIf(image == null, ErrorCode.PARAMS_ERROR, "Service: 图片对象为空");

        Long id = image.getId();
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "Service: 图片 id 为空");

        String url = image.getUrl();
        ThrowUtils.throwIf(StrUtil.isNotBlank(url) && url.length() > 1024,
                ErrorCode.PARAMS_ERROR, "Service: url 长度过长");

        String intro = image.getIntro();
        ThrowUtils.throwIf(StrUtil.isNotBlank(intro) && intro.length() > 1024,
                ErrorCode.PARAMS_ERROR, "Service: intro 长度过长");
    }


    @Override
    public void doImageReview(ImageReviewRequest imageReviewRequest, User loginUser) {
        // 1. 获得 id 和审核请求中的状态，校验参数
        Long id = imageReviewRequest.getId();
        Integer reviewStatus = imageReviewRequest.getReviewStatus();
        ImageReviewStatusEnum reviewStatusEnum = ImageReviewStatusEnum.getEnumByVal(reviewStatus);

        ThrowUtils.throwIf(id == null ||
                        reviewStatusEnum == null ||
                        ImageReviewStatusEnum.IN_REVIEW.equals(reviewStatusEnum),
                ErrorCode.PARAMS_ERROR, "Service: 更新审核状态请求为空");

        // 2. 判断图片是否存在
        Image oldImage = this.getById(id);
        ThrowUtils.throwIf(oldImage == null, ErrorCode.NOT_FOUND);
        // 3. 校验审核状态如果已经是该状态
        ThrowUtils.throwIf(oldImage.getReviewStatus().equals(reviewStatus),
                ErrorCode.PARAMS_ERROR, "Service: 请勿重复审核");

        // 4. 新建 image 对象，更新审核状态
        Image imageToUpdateStatus = new Image();
        BeanUtils.copyProperties(imageReviewRequest, imageToUpdateStatus);
        imageToUpdateStatus.setReviewerId(loginUser.getId());
        imageToUpdateStatus.setReviewTime(new Date());

        boolean isUpdateReview = this.updateById(imageToUpdateStatus);
        ThrowUtils.throwIf(!isUpdateReview, ErrorCode.OPERATION_ERROR, "Service: 图片更新审核状态不成功");
    }

    @Override
    public void addReviewParams(Image image, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            // 管理员则直接过审，填充完整审核相关属性参数
            image.setReviewStatus(ImageReviewStatusEnum.PASS.getVal());
            image.setReviewerId(loginUser.getId());
            image.setReviewMsg("管理员自动过审");
            image.setReviewTime(new Date());
        } else
            // 非管理员，请求创建或编辑图片时，image 的状态改为待审核
            image.setReviewStatus(ImageReviewStatusEnum.IN_REVIEW.getVal());
    }


}




