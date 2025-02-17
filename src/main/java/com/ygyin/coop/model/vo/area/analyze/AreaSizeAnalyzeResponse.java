package com.ygyin.coop.model.vo.area.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 空间图片文件大小分析响应类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AreaSizeAnalyzeResponse implements Serializable {

    /**
     * 图片文件大小范围
     */
    private String sizeRange;

    /**
     * 符合文件大小范围的图片数量
     */
    private Long totalNum;

    private static final long serialVersionUID = 1L;
}

