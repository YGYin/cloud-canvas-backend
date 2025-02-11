package com.ygyin.coop.model.dto.image;

import lombok.Data;

import java.io.Serializable;

@Data
public class ImageFetchRequest implements Serializable {
    /**
     * 搜索关键词
     */
    private String searchText;

    /**
     * 图片抓取数量
     */
    private Integer fetchNum = 10;

    /**
     * 抓取图片的命名名称前缀
     */
    private String namePrefix;

    private static final long serialVersionUID = 1L;
}
