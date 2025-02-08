package com.ygyin.coop.model.dto.image;

import lombok.Data;

import java.io.Serializable;

/**
 * 图片更新请求包装类
 */
@Data
public class ImageReviewRequest implements Serializable {

    /**
     * 图片 id
     */
    private Long id;

    /**
     * 状态：0-待审核; 1-通过; 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMsg;

    private static final long serialVersionUID = 1L;
}
