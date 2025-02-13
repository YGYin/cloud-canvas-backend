package com.ygyin.coop.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ygyin.coop.exception.BusinessException;
import com.ygyin.coop.exception.ErrorCode;
import com.ygyin.coop.exception.ThrowUtils;
import com.ygyin.coop.manager.CosManager;
import com.ygyin.coop.manager.upload.FileImageUpload;
import com.ygyin.coop.manager.upload.ImageUploadTemplate;
import com.ygyin.coop.manager.upload.UrlImageUpload;
import com.ygyin.coop.model.dto.file.UploadImageResult;
import com.ygyin.coop.model.dto.image.*;
import com.ygyin.coop.model.entity.Area;
import com.ygyin.coop.model.entity.Image;
import com.ygyin.coop.model.entity.User;
import com.ygyin.coop.model.enums.ImageReviewStatusEnum;
import com.ygyin.coop.model.vo.ImageVO;
import com.ygyin.coop.service.AreaService;
import com.ygyin.coop.service.ImageService;
import com.ygyin.coop.mapper.ImageMapper;
import com.ygyin.coop.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
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
@Slf4j
@Service
public class ImageServiceImpl extends ServiceImpl<ImageMapper, Image>
        implements ImageService {

    @Resource
    private FileImageUpload fileImageUpload;

    @Resource
    private UrlImageUpload urlImageUpload;

    @Resource
    private UserService userService;

    @Resource
    private AreaService areaService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private CosManager cosManager;

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
        // 1. 校验用户是否为空，同时需要校验空间是否存在，是否为当前空间管理员
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH);
        Long areaId = imageUploadRequest.getAreaId();
        if (areaId != null) {
            Area area = areaService.getById(areaId);
            ThrowUtils.throwIf(area == null,
                    ErrorCode.NOT_FOUND, "Service: 该空间不存在");
            // 检查权限
            ThrowUtils.throwIf(!loginUser.getId().equals(area.getUserId()),
                    ErrorCode.NO_AUTH, "Service: 没有权限上传文件到该空间");
            // 校验空间文件数量和空间限制
            ThrowUtils.throwIf(area.getTotalNum() >= area.getMaxNum(),
                    ErrorCode.OPERATION_ERROR, "Service: 空间已达最大文件数量");
            ThrowUtils.throwIf(area.getTotalSize() >= area.getMaxSize(),
                    ErrorCode.OPERATION_ERROR, "Service: 空间最大容量已满");
        }

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

            // 校验 area id 如果不为空时是否一致
            if (areaId != null)
                ThrowUtils.throwIf(!oldImage.getAreaId().equals(areaId),
                        ErrorCode.PARAMS_ERROR, "Service: 原图片空间 id 与上传空间 id 不一致");
            else {
                // area id 为空，直接复用原来 id，如果原 area id 为空说明上传到公共图库
                if (oldImage.getAreaId() != null)
                    areaId = oldImage.getAreaId();
            }
        }

        // 3. 上传图片，先判断空间 id 是否为空
        //    为空则先根据用户 id 或名称构造上传后的路径，
        //    不为空则按照空间 id 来划分目录，通过返回的上传结果得到图片信息
        // todo: 可改为名字
        String pathPrefix;
        if (areaId == null)
            pathPrefix = String.format("public/%s", loginUser.getId());
        else
            pathPrefix = String.format("area/%s", areaId);

        // 根据上传文件的类型，来区分上传文件的方式
        // 默认为文件类型，但如果上传的为 String 类型，说明为 url
        ImageUploadTemplate imageUploadTemplate = fileImageUpload;
        if (inputSource instanceof String)
            imageUploadTemplate = urlImageUpload;
        // 上传图片文件或 url
        UploadImageResult uploadImgResult = imageUploadTemplate.uploadImage(inputSource, pathPrefix);

        // 4. 根据图片信息构造 ImageVO
        Image image = new Image();
        // 加入 area id
        image.setAreaId(areaId);
        image.setUrl(uploadImgResult.getUrl());
        image.setThumbUrl(uploadImgResult.getThumbUrl());
        String imgName = uploadImgResult.getName();
        // 如果是抓取图片传入的 upload request，检查请求中用于前缀命名的图片名是否为空
        if (imageUploadRequest != null && StrUtil.isNotBlank(imageUploadRequest.getImgName()))
            imgName = imageUploadRequest.getImgName();
        image.setName(imgName);

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
        Long finalAreaId = areaId;
        transactionTemplate.execute(status -> {
            // 5.1 更新 image 数据库
            boolean isSaveOrUpdate = this.saveOrUpdate(image);
            // todo 如果是更新图片可删除 COS 中原图片
            // imageService.removeImageFileOnCOS(imgToUpdate);
            ThrowUtils.throwIf(!isSaveOrUpdate, ErrorCode.OPERATION_ERROR, "当前图片上传失败");

            // 5.2 不是公共空间时，才需要更新 area 的使用额度
            if (finalAreaId != null) {
                boolean isUpdate = areaService.lambdaUpdate()
                        .eq(Area::getId, finalAreaId)
                        .setSql("totalSize = totalSize + " + image.getImgSize())
                        .setSql("totalNum = totalNum + 1")
                        .update();
                ThrowUtils.throwIf(!isUpdate, ErrorCode.OPERATION_ERROR, "当前空间额度更新失败");
            }
            return image;
        });

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
        // 获取空间 id
        Long areaId = imageQueryRequest.getAreaId();
        boolean nullAreaId = imageQueryRequest.isNullAreaId();

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

        queryWrapper.eq(ObjectUtil.isNotEmpty(areaId), "areaId", areaId);
        queryWrapper.isNull(nullAreaId, "areaId");

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

    @Override
    public Integer uploadImageByFetch(ImageFetchRequest imageFetchRequest, User loginUser) {
        // 1. 校验请求参数
        String searchText = imageFetchRequest.getSearchText();
        Integer fetchNum = imageFetchRequest.getFetchNum();
        ThrowUtils.throwIf(fetchNum > 30, ErrorCode.PARAMS_ERROR, "Service: 最多一次性抓取 30 条");
        String namePrefix = imageFetchRequest.getNamePrefix();
        if (namePrefix.isEmpty())
            namePrefix = searchText;


        // 2. 获取要抓取图片的地址
        String urlToFetch = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        // 3. 使用 jsoup 对抓取地址发送请求
        Document doc;
        try {
            doc = Jsoup.connect(urlToFetch).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Service: 抓取页面失败");
        }

        // 4. 对获取的页面获取对应的图片 url 元素，获得 元素 list
        Element divElement = doc.getElementsByClass("dgControl").first();
        ThrowUtils.throwIf(ObjUtil.isNull(divElement),
                ErrorCode.OPERATION_ERROR, "Service: 抓取元素失败");

        Elements imgElements = divElement.select("img.mimg");
        int successNum = 0;

        // 5. 遍历元素 list 获取每张图片的 url，并对图片 url 进行处理，避免转义问题
        for (Element imgElem : imgElements) {
            String imgUrl = imgElem.attr("src");
            if (StrUtil.isBlank(imgUrl)) {
                log.info("该图片链接为空，已跳过: {}", imgUrl);
                continue;
            }
            // 对图片 url 中带的 '?' 进行处理，避免转义问题
            int questionIndex = imgUrl.indexOf("?");
            if (questionIndex > -1)
                imgUrl = imgUrl.substring(0, questionIndex);

            // 上传图片，构造文件名
            ImageUploadRequest imageUploadRequest = new ImageUploadRequest();
            imageUploadRequest.setUrl(imgUrl);
            imageUploadRequest.setImgName(namePrefix + (successNum + 1));
            try {
                // imageUploadRequest 用于判断是否已经有该图片，并用于构造上传文件名
                ImageVO imageVO = this.uploadImage(imgUrl, imageUploadRequest, loginUser);
                log.info("图片上传成功, id = {}", imageVO.getId());
                successNum++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            if (successNum >= fetchNum)
                break;
        }

        return successNum;
    }

    @Override
    public void editImage(ImageEditRequest imageEditRequest, User loginUser) {
        // 将 请求 dto 转换为 Image 实体类，tags 需要手动转为 Json String，并设置编辑时间
        Image image = new Image();
        BeanUtil.copyProperties(imageEditRequest, image);
        image.setTags(JSONUtil.toJsonStr(imageEditRequest.getTags()));
        image.setEditTime(new Date());
        // 图片数据校验
        this.verifyImage(image);

        // 判断更新的图片数据库中是否存在
        Long id = imageEditRequest.getId();
        Image imgToEdit = this.getById(id);
        ThrowUtils.throwIf(imgToEdit == null,
                ErrorCode.NOT_FOUND, "Service: 编辑图片不存在");

        // 校验用户权限，校验只有本人或管理员才可以对图片进行编辑
        this.checkImageOpsAuth(loginUser, imgToEdit);
        // 添加审核参数
        this.addReviewParams(image, loginUser);

        // 数据库更新图片
        boolean isEdit = this.updateById(image);
        ThrowUtils.throwIf(!isEdit,
                ErrorCode.OPERATION_ERROR, "Service: 未正确编辑该图片");
    }

    @Override
    public void deleteImage(long imgId, User loginUser) {
        ThrowUtils.throwIf(imgId <= 0 || loginUser == null,
                ErrorCode.PARAMS_ERROR, "Service: 删除图片参数不合法");
        // 判断删除的图片是否存在
        Image imgToDelete = this.getById(imgId);
        ThrowUtils.throwIf(imgToDelete == null,
                ErrorCode.NOT_FOUND, "Service: 删除的图片不存在");

        // 校验权限，只有上传该图片的本人或者管理员才可以删除
        this.checkImageOpsAuth(loginUser, imgToDelete);

        // 数据库删除该图片, todo 管理页面删除公共图片有可能额度异常
        transactionTemplate.execute(status -> {
            // 5.1 更新 image 数据库
            boolean isDelete = this.removeById(imgId);
            ThrowUtils.throwIf(!isDelete,
                    ErrorCode.OPERATION_ERROR, "Service: 未正确删除该图片");

            // 5.2 更新 area 的使用额度
            boolean isUpdate = areaService.lambdaUpdate()
                    .eq(Area::getId, imgToDelete.getAreaId())
                    .setSql("totalSize = totalSize + " + imgToDelete.getImgSize())
                    .setSql("totalNum = totalNum - 1")
                    .update();
            ThrowUtils.throwIf(!isUpdate, ErrorCode.OPERATION_ERROR, "当前空间额度更新失败");
            return true;
        });

        // 清理 COS 中的图片资源
        this.removeImageFileOnCOS(imgToDelete);
    }

    @Async
    @Override
    public void removeImageFileOnCOS(Image oldImage) {
        // 判断该图片是否被多条记录使用
        String imgUrl = oldImage.getUrl();
        long count = this.lambdaQuery()
                .eq(Image::getUrl, imgUrl)
                .count();
        // 有不止一条记录引用该图片 不清理
        if (count > 1)
            return;

        // 此处 url 包含了域名，实际上只要传 key 值（存储路径）就够了
        cosManager.deleteObject(oldImage.getUrl());
        // 清理缩略图
        String thumbUrl = oldImage.getThumbUrl();
        if (!thumbUrl.isEmpty())
            cosManager.deleteObject(thumbUrl);

    }

    @Override
    public void checkImageOpsAuth(User loginUser, Image image) {
        Long imgAreaId = image.getAreaId();
        Long loginUserId = loginUser.getId();

        // 如果为公共图库，只有本人或者管理员可以操作该图片
        if (imgAreaId == null)
            ThrowUtils.throwIf(!image.getUserId().equals(loginUserId) && !userService.isAdmin(loginUser),
                    ErrorCode.NO_AUTH, "Service: 无权限操作该图片");
        else
            ThrowUtils.throwIf(!image.getUserId().equals(loginUserId),
                    ErrorCode.NO_AUTH, "Service: 无权限操作该图片");
    }

}




