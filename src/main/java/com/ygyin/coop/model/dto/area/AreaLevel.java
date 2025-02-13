package com.ygyin.coop.model.dto.area;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class AreaLevel implements Serializable {

    /**
     * 枚举值
     */
    private int val;

    /**
     * 空间等级
     */
    private String text;

    /**
     * 最大文件数量
     */
    private long maxNum;

    /**
     * 最大容量
     */
    private long maxSize;

    private static final long serialVersionUID = 1L;
}