package com.ygyin.coop.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ygyin.coop.annotation.AuthVerify;
import com.ygyin.coop.common.BaseResponse;
import com.ygyin.coop.common.DeleteRequest;
import com.ygyin.coop.common.ResUtils;
import com.ygyin.coop.constant.UserConstant;
import com.ygyin.coop.exception.ErrorCode;
import com.ygyin.coop.exception.ThrowUtils;
import com.ygyin.coop.model.dto.image.*;
import com.ygyin.coop.model.entity.Area;
import com.ygyin.coop.model.entity.Image;
import com.ygyin.coop.model.entity.User;
import com.ygyin.coop.model.enums.ImageReviewStatusEnum;
import com.ygyin.coop.model.vo.ImageTagCategory;
import com.ygyin.coop.model.vo.ImageVO;
import com.ygyin.coop.service.AreaService;
import com.ygyin.coop.service.ImageService;
import com.ygyin.coop.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/image")
@Slf4j
public class ImageController {

    @Resource
    private UserService userService;

    @Resource
    private ImageService imageService;

    @Resource
    private AreaService areaService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private final Cache<String, String> LOCAL_CACHE =
            Caffeine.newBuilder().initialCapacity(1024)
                    .maximumSize(10000L)
                    // 缓存 5 分钟移除
                    .expireAfterWrite(5L, TimeUnit.MINUTES)
                    .build();


    /**
     * 上传图片
     */
    @PostMapping("/upload")
//    @AuthVerify(requiredRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<ImageVO> uploadImage(
            @RequestPart("file") MultipartFile multipartFile,
            ImageUploadRequest imageUploadRequest,
            HttpServletRequest request
    ) {
        // 先获取登录用户，再调用 ImageService 上传图片
        User loginUser = userService.getLoginUser(request);
        ImageVO imageVO = imageService.uploadImage(multipartFile, imageUploadRequest, loginUser);

        return ResUtils.success(imageVO);
    }

    /**
     * 通过 URL 上传图片（可重新上传）
     */
    @PostMapping("/upload/url")
    public BaseResponse<ImageVO> uploadImageByUrl(
            @RequestBody ImageUploadRequest imageUploadRequest,
            HttpServletRequest request) {
        // 先获取登录用户，再调用 ImageService 上传图片
        User loginUser = userService.getLoginUser(request);
        // 获取文件 url
        String fileUrl = imageUploadRequest.getUrl();
        ImageVO imageVO = imageService.uploadImage(fileUrl, imageUploadRequest, loginUser);
        return ResUtils.success(imageVO);
    }


    /**
     * 删除图片
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteImage(
            @RequestBody DeleteRequest deleteRequest,
            HttpServletRequest request) {
        // 对请求判空
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR, "Controller: 删除图片请求为空");
        // 判断删除的图片是否存在
        Long id = deleteRequest.getId();
        User loginUser = userService.getLoginUser(request);
        imageService.deleteImage(id, loginUser);
        return ResUtils.success(true);
    }

    /**
     * 更新图片（管理员）
     */
    @PostMapping("/update")
    @AuthVerify(requiredRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateImage(@RequestBody ImageUpdateRequest imageUpdateRequest,
                                             HttpServletRequest request) {
        // 请求判空
        ThrowUtils.throwIf(imageUpdateRequest == null || imageUpdateRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR, "Controller: 管理员更新图片请求为空");
        // 将 请求 dto 转换为 Image 实体类，tags 需要手动转为 Json String
        Image image = new Image();
        BeanUtil.copyProperties(imageUpdateRequest, image);
        image.setTags(JSONUtil.toJsonStr(imageUpdateRequest.getTags()));
        // 图片数据校验，判断更新的图片是否存在
        imageService.verifyImage(image);
        Long id = imageUpdateRequest.getId();
        Image imgToUpdate = imageService.getById(id);
        ThrowUtils.throwIf(imgToUpdate == null,
                ErrorCode.NOT_FOUND, "Controller: 更新图片不存在");
        // 添加审核参数
        User loginUser = userService.getLoginUser(request);
        imageService.addReviewParams(image, loginUser);

        // 数据库更新图片
        boolean isUpdate = imageService.updateById(image);
        ThrowUtils.throwIf(!isUpdate, ErrorCode.OPERATION_ERROR, "Controller: 未正确更新该图片");
        return ResUtils.success(true);
    }

    /**
     * 根据 id 获取图片（管理员）
     */
    @GetMapping("/get")
    @AuthVerify(requiredRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Image> getImageById(long imgId, HttpServletRequest request) {
        // 对 imgId 判空
        ThrowUtils.throwIf(imgId <= 0,
                ErrorCode.PARAMS_ERROR, "Controller: 获取图片 id 不合法");

        // 数据库查询图片
        Image image = imageService.getById(imgId);
        ThrowUtils.throwIf(image == null,
                ErrorCode.NOT_FOUND, "Controller: 该图片不存在");
        return ResUtils.success(image);
    }

    /**
     * 根据 id 获取图片封装类
     */
    @GetMapping("/get/vo")
    public BaseResponse<ImageVO> getImageVOById(long imgId, HttpServletRequest request) {
        // 对 imgId 判空
        ThrowUtils.throwIf(imgId <= 0,
                ErrorCode.PARAMS_ERROR, "Controller: 获取图片 id 不合法");

        // 数据库查询图片
        Image image = imageService.getById(imgId);
        ThrowUtils.throwIf(image == null,
                ErrorCode.NOT_FOUND, "Controller: 该图片不存在");

        // 需要对空间权限进行校验，如果 areaId 不为空图片说明在私人空间
        Long areaId = image.getAreaId();
        if (areaId != null) {
            User loginUser = userService.getLoginUser(request);
            imageService.checkImageOpsAuth(loginUser, image);
        }

        return ResUtils.success(imageService.getImageVO(image, request));
    }

    /**
     * 分页获取图片列表（管理员）
     */
    @PostMapping("/list/page")
    @AuthVerify(requiredRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Image>> listImageByPage(@RequestBody ImageQueryRequest imageQueryRequest) {
        // 获取 current 和 size
        int current = imageQueryRequest.getCurrentPage();
        int pageSize = imageQueryRequest.getPageSize();
        // 分页查询数据库
        Page<Image> imagePage = imageService.page(new Page<Image>(current, pageSize),
                imageService.getQueryWrapper(imageQueryRequest));
        return ResUtils.success(imagePage);
    }

    /**
     * 分页获取图片列表（封装类）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<ImageVO>> listImageVOByPage(@RequestBody ImageQueryRequest imageQueryRequest,
                                                         HttpServletRequest request) {
        // 获取 current 和 size
        int current = imageQueryRequest.getCurrentPage();
        int pageSize = imageQueryRequest.getPageSize();
        // 对用户分页请求的 pageSize 进行限制，防止其进行爬虫
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "Controller: 非法参数");

        // 根据 areaId 区分公共空间和私有空间
        Long areaId = imageQueryRequest.getAreaId();
        if (areaId != null) {
            User loginUser = userService.getLoginUser(request);
            Area area = areaService.getById(areaId);
            // 校验用户空间权限
            ThrowUtils.throwIf(area == null,
                    ErrorCode.NOT_FOUND, "Controller: 该空间不存在");
            ThrowUtils.throwIf(!area.getUserId().equals(loginUser.getId()),
                    ErrorCode.NO_AUTH, "Controller: 无权限访问该空间");
        } else {
            // 公共图库，用户可以看到经过审核的数据
            // 对查询结果进行过滤，只允许普通用户查询到审核通过的图片
            imageQueryRequest.setReviewStatus(ImageReviewStatusEnum.PASS.getVal());
            imageQueryRequest.setNullAreaId(true);
        }

        // 分页查询数据库
        Page<Image> imagePage = imageService.page(new Page<Image>(current, pageSize),
                imageService.getQueryWrapper(imageQueryRequest));

        // 转为 VO Page 返回
        return ResUtils.success(imageService.getImageVOPage(imagePage, request));
    }

    /**
     * 通过缓存分页获取图片列表（封装类）
     */
    @Deprecated
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<ImageVO>> listImageVOByPageWithCache(@RequestBody ImageQueryRequest imageQueryRequest,
                                                                  HttpServletRequest request) {
        // 获取 current 和 size
        int current = imageQueryRequest.getCurrentPage();
        int pageSize = imageQueryRequest.getPageSize();
        // 对用户分页请求的 pageSize 进行限制，防止其进行爬虫
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "Controller: 非法参数");

        // 对查询结果进行过滤，只允许普通用户查询到审核通过的图片
        imageQueryRequest.setReviewStatus(ImageReviewStatusEnum.PASS.getVal());

        // 1. 先通过本地及 redis 缓存来查询，构建缓存的 key，将序列化后经过 md5 压缩的查询条件作为 key
        String query = JSONUtil.toJsonStr(imageQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(query.getBytes());
        String key = "coop:listImageVOByPage:" + hashKey;
        // 1.1 从本地缓存中查询，如果本地缓存命中，反序列化返回结果
        String valInCache = LOCAL_CACHE.getIfPresent(key);
        if (valInCache != null) {
            Page<ImageVO> pageInCache = JSONUtil.toBean(valInCache, Page.class);
            return ResUtils.success(pageInCache);
        }
        // 2. 本地缓存没命中，查 redis
        ValueOperations<String, String> valOps = stringRedisTemplate.opsForValue();
        valInCache = valOps.get(key);
        // 如果缓存命中，更新本地缓存后，反序列化后直接返回结果
        if (valInCache != null) {
            LOCAL_CACHE.put(key, valInCache);
            Page<ImageVO> pageInCache = JSONUtil.toBean(valInCache, Page.class);
            return ResUtils.success(pageInCache);
        }

        // 3. 没有命中，分页查询数据库，序列化后并存入本地和 redis 缓存
        Page<Image> imagePage = imageService.page(new Page<Image>(current, pageSize),
                imageService.getQueryWrapper(imageQueryRequest));
        // 获取分页封装类
        Page<ImageVO> imageVOPage = imageService.getImageVOPage(imagePage, request);
        // 3.1 更新 redis 缓存
        String valToCache = JSONUtil.toJsonStr(imageVOPage);
        // 设置随机过期时间，防止缓存雪崩，即使查询结果是空也会设置到缓存中，避免缓存穿透
        int timeout = 60 + RandomUtil.randomInt(0, 120);
        valOps.set(key, valToCache, timeout, TimeUnit.SECONDS);

        // 3.2 写入本地缓存
        LOCAL_CACHE.put(key, valToCache);
        // 转为 VO Page 返回
        return ResUtils.success(imageVOPage);
    }


    /**
     * 编辑图片（用户）
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editImage(@RequestBody ImageEditRequest imageEditRequest, HttpServletRequest request) {
        // 请求判空
        ThrowUtils.throwIf(imageEditRequest == null || imageEditRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR, "Controller: 编辑图片请求为空");

        User loginUser = userService.getLoginUser(request);
        imageService.editImage(imageEditRequest, loginUser);
        return ResUtils.success(true);
    }


    @GetMapping("/tag_category")
    public BaseResponse<ImageTagCategory> listImageTagCategory() {
        ImageTagCategory imgTagCategory = new ImageTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        imgTagCategory.setTagList(tagList);
        imgTagCategory.setCategoryList(categoryList);
        return ResUtils.success(imgTagCategory);
    }

    @PostMapping("/review")
    @AuthVerify(requiredRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doImageReview(@RequestBody ImageReviewRequest imageReviewRequest,
                                               HttpServletRequest request) {
        ThrowUtils.throwIf(imageReviewRequest == null,
                ErrorCode.PARAMS_ERROR, "Controller: 审核请求为空");

        // 获取登录用户，调用 service
        User loginUser = userService.getLoginUser(request);
        imageService.doImageReview(imageReviewRequest, loginUser);
        return ResUtils.success(true);
    }

    @PostMapping("/upload/fetch")
    @AuthVerify(requiredRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadImageByFetch(@RequestBody ImageFetchRequest imageFetchRequest,
                                                    HttpServletRequest request) {
        ThrowUtils.throwIf(imageFetchRequest == null,
                ErrorCode.PARAMS_ERROR, "Controller: 抓取图片请求为空");

        // 获取登录用户，调用 service
        User loginUser = userService.getLoginUser(request);
        Integer successFetchNum = imageService.uploadImageByFetch(imageFetchRequest, loginUser);
        return ResUtils.success(successFetchNum);
    }

    @PostMapping("/search/color")
    public BaseResponse<List<ImageVO>> searchImageByColor(
            @RequestBody SearchImageByColorRequest searchImageByColorRequest,
            HttpServletRequest request) {
        // 请求判空
        ThrowUtils.throwIf(searchImageByColorRequest == null,
                ErrorCode.PARAMS_ERROR, "Controller: 通过颜色相似度搜索图片请求为空");
        // 获取参数
        String picColor = searchImageByColorRequest.getImgColor();
        Long areaId = searchImageByColorRequest.getAreaId();

        User loginUser = userService.getLoginUser(request);
        List<ImageVO> imgComparedList = imageService.searchImageByColorSimilar(areaId, picColor, loginUser);
        return ResUtils.success(imgComparedList);
    }

    /**
     * 批量更新图片分类和标签
     *
     * @param imageBatchEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit/batch")
    public BaseResponse<Boolean> batchEditImage(
            @RequestBody ImageBatchEditRequest imageBatchEditRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(imageBatchEditRequest == null,
                ErrorCode.PARAMS_ERROR, "Controller: 批量修改图片请求为空");
        User loginUser = userService.getLoginUser(request);
        imageService.batchEditImage(imageBatchEditRequest, loginUser);
        return ResUtils.success(true);
    }
}
