package com.ygyin.coop.model.dto.area.analyze;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 通用分析请求，用于分析空间不同数据
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AreaUsageAnalyzeRequest extends AreaAnalyzeRequest {
}