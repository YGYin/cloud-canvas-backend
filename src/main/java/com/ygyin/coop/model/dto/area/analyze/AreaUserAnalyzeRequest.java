package com.ygyin.coop.model.dto.area.analyze;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 空间用户上传行为分析请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AreaUserAnalyzeRequest extends AreaAnalyzeRequest {

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 时间范围字符串，分为日，周，月
     */
    private String durationStr;
}