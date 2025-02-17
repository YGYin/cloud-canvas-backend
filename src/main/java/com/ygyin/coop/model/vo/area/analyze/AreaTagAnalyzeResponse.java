package com.ygyin.coop.model.vo.area.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 空间 tag 分析响应类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AreaTagAnalyzeResponse implements Serializable {

    /**
     * 当前标签
     */
    private String tag;

    /**
     * 标签使用数量
     */
    private Long tagUsedNum;

    private static final long serialVersionUID = 1L;
}

