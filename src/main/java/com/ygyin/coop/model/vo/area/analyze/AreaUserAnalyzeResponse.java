package com.ygyin.coop.model.vo.area.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 空间用户上传行为分析响应类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AreaUserAnalyzeResponse implements Serializable {

    /**
     * 具体时间范围
     */
    private String timeDuration;

    /**
     * 用户在该时间范围内的文件上传数量
     */
    private Long uploadNum;

    private static final long serialVersionUID = 1L;
}

