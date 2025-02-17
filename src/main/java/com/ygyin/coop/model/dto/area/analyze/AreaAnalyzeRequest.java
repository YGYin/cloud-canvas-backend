package com.ygyin.coop.model.dto.area.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用分析请求，用于分析空间不同数据
 */
@Data
public class AreaAnalyzeRequest implements Serializable {

    /**
     * 空间 ID
     */
    private Long areaId;

    /**
     * 是否查询公共图库
     */
    private boolean analyzePublic;

    /**
     * 分析全部空间
     */
    private boolean analyzeAll;

    private static final long serialVersionUID = 1L;
}