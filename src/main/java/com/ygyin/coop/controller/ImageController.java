package com.ygyin.coop.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ygyin.coop.annotation.AuthVerify;
import com.ygyin.coop.common.BaseResponse;
import com.ygyin.coop.common.DeleteRequest;
import com.ygyin.coop.common.ResUtils;
import com.ygyin.coop.constant.UserConstant;
import com.ygyin.coop.exception.ErrorCode;
import com.ygyin.coop.exception.ThrowUtils;
import com.ygyin.coop.model.dto.image.ImageEditRequest;
import com.ygyin.coop.model.dto.image.ImageQueryRequest;
import com.ygyin.coop.model.dto.image.ImageUpdateRequest;
import com.ygyin.coop.model.dto.image.ImageUploadRequest;
import com.ygyin.coop.model.entity.Image;
import com.ygyin.coop.model.entity.User;
import com.ygyin.coop.model.vo.ImageTagCategory;
import com.ygyin.coop.model.vo.ImageVO;
import com.ygyin.coop.service.ImageService;
import com.ygyin.coop.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/image")
@Slf4j
public class ImageController {

    @Resource
    private UserService userService;

    @Resource
    private ImageService imageService;

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
        Image imgToDelete = imageService.getById(id);
        ThrowUtils.throwIf(imgToDelete == null,
                ErrorCode.NOT_FOUND, "Controller: 删除的图片不存在");

        // 校验权限，只有上传该图片的本人或者管理员才可以删除
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(!imgToDelete.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser),
                ErrorCode.NO_AUTH, "Controller: 当前用户无权限删除该图片");

        // 数据库删除该图片
        boolean isDelete = imageService.removeById(id);
        ThrowUtils.throwIf(!isDelete, ErrorCode.OPERATION_ERROR, "Controller: 未正确删除该图片");
        return ResUtils.success(true);
    }

    /**
     * 更新图片（管理员）
     */
    @PostMapping("/update")
    @AuthVerify(requiredRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateImage(@RequestBody ImageUpdateRequest imageUpdateRequest) {
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
        // 分页查询数据库
        Page<Image> imagePage = imageService.page(new Page<Image>(current, pageSize),
                imageService.getQueryWrapper(imageQueryRequest));

        // 转为 VO Page 返回
        return ResUtils.success(imageService.getImageVOPage(imagePage, request));
    }


    /**
     * 编辑图片（用户）
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editImage(@RequestBody ImageEditRequest imageEditRequest, HttpServletRequest request) {
        // 请求判空
        ThrowUtils.throwIf(imageEditRequest == null || imageEditRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR, "Controller: 编辑图片请求为空");
        // 将 请求 dto 转换为 Image 实体类，tags 需要手动转为 Json String，并设置编辑时间
        Image image = new Image();
        BeanUtil.copyProperties(imageEditRequest, image);
        image.setTags(JSONUtil.toJsonStr(imageEditRequest.getTags()));
        image.setEditTime(new Date());
        // 图片数据校验，判断更新的图片是否存在
        imageService.verifyImage(image);
        Long id = imageEditRequest.getId();
        Image imgToEdit = imageService.getById(id);
        ThrowUtils.throwIf(imgToEdit == null,
                ErrorCode.NOT_FOUND, "Controller: 编辑图片不存在");

        // 获取登录用户，校验只有本人或管理员才可以对图片进行编辑
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(!imgToEdit.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser),
                ErrorCode.NO_AUTH, "Controller: 当前用户无权限编辑该图片");

        // 数据库更新图片
        boolean isEdit = imageService.updateById(image);
        ThrowUtils.throwIf(!isEdit, ErrorCode.OPERATION_ERROR, "Controller: 未正确编辑该图片");
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


}
