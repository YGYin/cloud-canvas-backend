package com.ygyin.coop.model.vo.area.analyze;

import lombok.Data;

import java.io.Serializable;

@Data
public class AreaUsageAnalyzeResponse implements Serializable {
    /**
     * 已使用大小
     */
    private Long usedSize;

    /**
     * 总大小
     */
    private Long maxSize;

    /**
     * 已用空间占比
     */
    private Double usedSizePercent;

    /**
     * 当前已用图片数量
     */
    private Long usedNum;

    /**
     * 最大图片数量
     */
    private Long maxNum;

    /**
     * 已用图片数量占比
     */
    private Double usedNumPercent;

    private static final long serialVersionUID = 1L;
}

