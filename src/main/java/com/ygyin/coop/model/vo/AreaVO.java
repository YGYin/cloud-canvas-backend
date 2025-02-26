package com.ygyin.coop.model.vo;

import cn.hutool.core.bean.BeanUtil;
import com.ygyin.coop.model.entity.Area;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 空间 VO 类
 */
@Data
public class AreaVO implements Serializable {
    /**
     * id
     */
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
     * 空间类型：0-private 1-team
     */
    private Integer areaType;


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
     * 创建该空间的用户信息
     */
    private UserVO user;

    private static final long serialVersionUID = 1L;

    /**
     * 空间 VO 封装类转对象
     *
     * @param areaVO
     * @return
     */
    public static Area voToObj(AreaVO areaVO) {
        if (areaVO == null)
            return null;

        Area area = new Area();
        BeanUtil.copyProperties(areaVO, area);
        return area;
    }

    /**
     * 空间对象转 VO 封装类
     *
     * @param area
     * @return
     */
    public static AreaVO objToVo(Area area) {
        if (area == null)
            return null;

        AreaVO areaVO = new AreaVO();
        BeanUtil.copyProperties(area, areaVO);
        return areaVO;
    }
}