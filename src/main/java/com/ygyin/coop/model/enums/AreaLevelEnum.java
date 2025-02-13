package com.ygyin.coop.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum AreaLevelEnum {
    DEFAULT("Default", 0, 100, 100L * 1024 * 1024),
    PROFESSIONAL("Pro", 1, 1000, 1000L * 1024 * 1024),
    ULTRA("Ultra", 2, 10000, 10000L * 1024 * 1024);

    private final String text;

    private final int val;

    private final long maxNum;

    private final long maxSize;

    AreaLevelEnum(String text, int val, long maxNum, long maxSize) {
        this.text = text;
        this.val = val;
        this.maxNum = maxNum;
        this.maxSize = maxSize;
    }

    /**
     * 通过 value 查找枚举对象
     *
     * @param val
     * @return 枚举对象
     */
    public static AreaLevelEnum getEnumByVal(Integer val) {
        if (val == null)
            return null;

        for (AreaLevelEnum areaLevelEnum : AreaLevelEnum.values())
            if (areaLevelEnum.val == val)
                return areaLevelEnum;

        return null;
    }

}
