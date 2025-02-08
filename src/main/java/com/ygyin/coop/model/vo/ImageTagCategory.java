package com.ygyin.coop.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 图片标签及分类
 */
@Data
public class ImageTagCategory {

    /**
     * 标签列表
     */
    private List<String> tagList;

    /**
     * 分类列表
     */
    private List<String> categoryList;
}
