package com.ygyin.coop.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 空间类型枚举类，0-私有空间，1-团队空间
 */
@Getter
public enum AreaTypeEnum {
    PRIVATE("Private Area", 0),
    TEAM("Team Area", 1);

    private final String text;

    private final int val;

    AreaTypeEnum(String text, int val) {
        this.text = text;
        this.val = val;
    }

    /**
     * 根据 val 获取枚举
     */
    public static AreaTypeEnum getEnumByVal(Integer val) {
        if (ObjUtil.isEmpty(val))
            return null;

        for (AreaTypeEnum spaceTypeEnum : AreaTypeEnum.values())
            if (spaceTypeEnum.val == val)
                return spaceTypeEnum;

        return null;
    }
}

