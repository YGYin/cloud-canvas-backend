package com.ygyin.coop.model.dto.image;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 图片更新请求包装类
 */
@Data
public class ImageUpdateRequest implements Serializable {

    /**
     * 图片 id
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

    private static final long serialVersionUID = 1L;
}
