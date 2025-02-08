package com.ygyin.coop.model.dto.file;

import lombok.Data;

/**
 * 上传图片结果，在调用完上传图片后，接收得到该结果
 */
@Data
public class UploadImageResult {

    /**
     * 图片 url
     */
    private String url;

    /**
     * 图片名称
     */
    private String name;

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

}