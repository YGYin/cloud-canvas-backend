package com.ygyin.coop.common;

import lombok.Data;

/**
 * 通用分页请求封装类，用于接收前端分页请求
 */
@Data
public class PageRequest {
    /**
     * 当前页号
     */
    private int currentPage = 1;

    /**
     * 页面大小
     */
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序
     */
    private String sortOrder = "desc";
}
