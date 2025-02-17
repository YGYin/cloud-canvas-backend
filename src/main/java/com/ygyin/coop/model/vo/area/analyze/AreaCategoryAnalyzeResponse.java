package com.ygyin.coop.model.vo.area.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 图片分类分析响应类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AreaCategoryAnalyzeResponse implements Serializable {

    /**
     * 当前图片分类
     */
    private String category;

    /**
     * 当前分类图片占用容量大小
     */
    private Long totalSize;

    /**
     * 当前分类图片数量
     */
    private Long totalNum;

    private static final long serialVersionUID = 1L;
}

