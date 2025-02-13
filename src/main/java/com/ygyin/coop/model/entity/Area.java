package com.ygyin.coop.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 空间
 * @TableName area
 */
@TableName(value ="area")
@Data
public class Area implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 个人空间名称
     */
    private String areaName;

    /**
     * 空间级别：0-default 1-Pro 2-Ultra
     */
    private Integer areaLevel;

    /**
     * 空间容量最大限制
     */
    private Long maxSize;

    /**
     * 空间图片最大数量
     */
    private Long maxNum;

    /**
     * 当前空间已使用容量
     */
    private Long totalSize;

    /**
     * 当前空间图片数量
     */
    private Long totalNum;

    /**
     * 创建用户 id
     */
    private Long userId;

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