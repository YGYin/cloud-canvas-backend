package com.ygyin.coop.model.dto.image;

import lombok.Data;

import java.io.Serializable;

/**
 * 使用图片颜色相似度进行搜索请求
 */
@Data
public class SearchImageByColorRequest implements Serializable {
    /**
     * 图片颜色
     */
    private String imgColor;

    /**
     * 个人空间 id
     */
    private Long areaId;

    private static final long serialVersionUID = 1L;

}