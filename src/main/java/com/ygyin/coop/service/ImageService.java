package com.ygyin.coop.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ygyin.coop.model.dto.image.ImageQueryRequest;
import com.ygyin.coop.model.dto.image.ImageReviewRequest;
import com.ygyin.coop.model.dto.image.ImageUploadRequest;
import com.ygyin.coop.model.entity.Image;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ygyin.coop.model.entity.User;
import com.ygyin.coop.model.vo.ImageVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

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
    public QueryWrapper<Image> getQueryWrapper(ImageQueryRequest imageQueryRequest);


    /**
     * 获取封装后的图片 VO 对象，并为图片关联创建用户的信息
     *
     * @param image
     * @param request
     * @return
     */
    public ImageVO getImageVO(Image image, HttpServletRequest request);

    /**
     * 分页获取封装后的图片 VO 对象
     *
     * @param imagePage
     * @param request
     * @return
     */
    public Page<ImageVO> getImageVOPage(Page<Image> imagePage, HttpServletRequest request);

    /**
     * 校验图片的 id, url 和 简介等参数是否合法
     *
     * @param image
     */
    public void verifyImage(Image image);

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
}
