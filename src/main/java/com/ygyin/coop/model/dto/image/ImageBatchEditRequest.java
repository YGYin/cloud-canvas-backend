package com.ygyin.coop.model.dto.image;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ImageBatchEditRequest implements Serializable {

    /**
     * 图片 id 列表
     */
    private List<Long> imgIdList;

    /**
     * 空间 id
     */
    private Long areaId;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签
     */
    private List<String> tags;

    /**
     * 批量命名规则
     */
    private String nameNorm;

    private static final long serialVersionUID = 1L;
}
