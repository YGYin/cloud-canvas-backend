package com.ygyin.coop.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * 图片
 *
 * @TableName image
 */
@TableName(value = "image")
@Data
public class Image implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 图片 url
     */
    private String url;

    /**
     * 缩略图 url
     */
    private String thumbUrl;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String intro;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签（JSON 数组）
     */
    private String tags;

    /**
     * 图片体积
     */
    private Long imgSize;

    /**
     * 图片宽度
     */
    private Integer imgWidth;

    /**
     * 图片高度
     */
    private Integer imgHeight;

    /**
     * 图片宽高比
     */
    private Double imgScale;

    /**
     * 图片格式
     */
    private String imgFormat;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 审核状态：0-待审核; 1-通过; 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMsg;

    /**
     * 审核人 ID
     */
    private Long reviewerId;

    /**
     * 审核时间
     */
    private Date reviewTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}