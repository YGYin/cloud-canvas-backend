package com.ygyin.coop.model.dto.image;

import com.ygyin.coop.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 用户查询请求封装类(管理员)
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ImageQueryRequest extends PageRequest implements Serializable {
    /**
     * id
     */
    private Long id;

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
     * 搜索关键字
     */
    private String searchText;

    /**
     * 用户 id
     */
    private Long userId;

    private static final long serialVersionUID = 1L;
}