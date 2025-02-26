package com.ygyin.coop.model.vo;

import com.ygyin.coop.model.entity.AreaUser;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

@Data
public class AreaUserVO implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 空间 id
     */
    private Long areaId;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String areaRole;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 用户 VO
     */
    private UserVO user;

    /**
     * 空间 VO
     */
    private AreaVO area;

    private static final long serialVersionUID = 1L;

    /**
     * 封装类转对象
     *
     * @param areaUserVO
     * @return
     */
    public static AreaUser voToObj(AreaUserVO areaUserVO) {
        if (areaUserVO == null)
            return null;

        AreaUser areaUser = new AreaUser();
        BeanUtils.copyProperties(areaUserVO, areaUser);
        return areaUser;
    }

    /**
     * 对象转封装类
     *
     * @param areaUser
     * @return
     */
    public static AreaUserVO objToVo(AreaUser areaUser) {
        if (areaUser == null)
            return null;

        AreaUserVO areaUserVO = new AreaUserVO();
        BeanUtils.copyProperties(areaUser, areaUserVO);
        return areaUserVO;
    }
}

