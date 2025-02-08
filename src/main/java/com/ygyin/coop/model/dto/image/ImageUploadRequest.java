package com.ygyin.coop.model.dto.image;

import lombok.Data;

import java.io.Serializable;

/**
 * 图片上传请求包装类 (类似 draft 或 temp file，非创建)
 */
@Data
public class ImageUploadRequest implements Serializable {

    /**
     * 图片 id，用于修改图片
     */
    private Long id;

    /**
     * 图片文件 url
     */
    private String url;

    private static final long serialVersionUID = -5864496671785854479L;
}
