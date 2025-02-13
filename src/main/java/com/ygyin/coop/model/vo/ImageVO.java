package com.ygyin.coop.model.vo;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.annotation.*;
import com.ygyin.coop.model.entity.Image;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 图片 VO
 */
@Data
public class ImageVO implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 图片 url
     */
    private String url;

    /**
     * 缩略图 url
     */
    private String thumbUrl;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String intro;

    /**
     * 分类
     */
    private String category;

    /**
     * 图片标签
     */
    private List<String> tags;

    /**
     * 图片体积
     */
    private Long imgSize;

    /**
     * 图片宽度
     */
    private Integer imgWidth;

    /**
     * 图片高度
     */
    private Integer imgHeight;

    /**
     * 图片宽高比
     */
    private Double imgScale;

    /**
     * 图片格式
     */
    private String imgFormat;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 个人空间 id
     */
    private Long areaId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    private UserVO userVO;

    private static final long serialVersionUID = 1L;

    /**
     * 图片对象封装为 VO
     *
     * @param image 图片对象
     * @return 图片 VO
     */
    public static ImageVO objToVo(Image image) {
        if (image == null)
            return null;
        // 新建 imgVO 对象，利用 BeanUtil 复制属性
        ImageVO imageVO = new ImageVO();
        BeanUtil.copyProperties(image, imageVO);
        // tags 类型不同，将 Json String 手动转换为 List<String>
        imageVO.setTags(JSONUtil.toList(image.getTags(), String.class));

        return imageVO;
    }

    /**
     * 图片 VO 转对象
     *
     * @param imageVO 图片 VO
     * @return 图片对象
     */
    public static Image voToObj(ImageVO imageVO) {
        if (imageVO == null)
            return null;
        // 新建 img 对象，利用 BeanUtil 复制属性
        Image image = new Image();
        BeanUtil.copyProperties(imageVO, image);
        // tags 类型不同，将 List<String> 手动转换为 String
        image.setTags(JSONUtil.toJsonStr(imageVO.getTags()));

        return image;
    }


}