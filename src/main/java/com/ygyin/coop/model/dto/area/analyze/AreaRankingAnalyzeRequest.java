package com.ygyin.coop.model.dto.area.analyze;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 空间按使用量排行分析请求
 */
@Data
public class AreaRankingAnalyzeRequest implements Serializable {

    /**
     * 排名前 N (默认为5) 的空间
     */
    private Integer topN = 5;

    private static final long serialVersionUID = 1L;
}