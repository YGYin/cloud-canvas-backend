package com.ygyin.coop.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ygyin.coop.api.aliyun.model.NewOutPaintingTaskResponse;
import com.ygyin.coop.model.dto.image.*;
import com.ygyin.coop.model.entity.Image;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ygyin.coop.model.entity.User;
import com.ygyin.coop.model.vo.ImageVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author yg
 * @description 针对表【image(图片)】的数据库操作Service
 * @createDate 2025-01-29 17:07:54
 */
public interface ImageService extends IService<Image> {

    /**
     * 上传图片
     *
     * @param inputSource        输入源文件(图片文件 / url)
     * @param imageUploadRequest 图片上传请求封装类
     * @param loginUser          用户
     * @return
     */
    ImageVO uploadImage(Object inputSource,
                        ImageUploadRequest imageUploadRequest,
                        User loginUser);


    /**
     * 根据查询图片的请求构造查询条件
     *
     * @param imageQueryRequest 图片查询请求
     * @return
     */
    QueryWrapper<Image> getQueryWrapper(ImageQueryRequest imageQueryRequest);


    /**
     * 获取封装后的图片 VO 对象，并为图片关联创建用户的信息
     *
     * @param image
     * @param request
     * @return
     */
    ImageVO getImageVO(Image image, HttpServletRequest request);

    /**
     * 分页获取封装后的图片 VO 对象
     *
     * @param imagePage
     * @param request
     * @return
     */
    Page<ImageVO> getImageVOPage(Page<Image> imagePage, HttpServletRequest request);

    /**
     * 校验图片的 id, url 和 简介等参数是否合法
     *
     * @param image
     */
    void verifyImage(Image image);

    /**
     * 图片审核
     *
     * @param imageReviewRequest
     * @param loginUser
     */
    void doImageReview(ImageReviewRequest imageReviewRequest, User loginUser);

    /**
     * 为上传，编辑，更新等操作的 image 对象添加审核参数
     *
     * @param image
     * @param loginUser
     */
    void addReviewParams(Image image, User loginUser);

    /**
     * 批量抓取和创建图片
     *
     * @param imageFetchRequest
     * @param loginUser
     * @return 成功抓取上传的图片数
     */
    Integer uploadImageByFetch(ImageFetchRequest imageFetchRequest, User loginUser);

    /**
     * 编辑图片
     *
     * @param imageEditRequest
     * @param loginUser
     */
    void editImage(ImageEditRequest imageEditRequest, User loginUser);

    /**
     * 删除图片
     *
     * @param imgId
     * @param loginUser
     */
    void deleteImage(long imgId, User loginUser);

    /**
     * 删除对象存储中的图片文件
     *
     * @param oldImage
     */
    void removeImageFileOnCOS(Image oldImage);

    /**
     * 校验用户是否有权限操作该图片
     *
     * @param loginUser
     * @param image
     */
    void checkImageOpsAuth(User loginUser, Image image);

    /**
     * 根据 RGB 颜色来搜索颜色相近的图片
     * @param areaId
     * @param imgColor
     * @param loginUser
     * @return
     */
    List<ImageVO> searchImageByColorSimilar(Long areaId, String imgColor, User loginUser);

    /**
     * 批量编辑
     *
     * @param imageBatchEditRequest
     * @param loginUser
     */
    void batchEditImage(ImageBatchEditRequest imageBatchEditRequest, User loginUser);

    /**
     * 新建扩图任务
     *
     * @param imgOutPaintingTaskRequest
     * @param loginUser
     * @return
     */
    NewOutPaintingTaskResponse newImageOutPaintingTask(NewImageOutPaintingTaskRequest imgOutPaintingTaskRequest, User loginUser);
}