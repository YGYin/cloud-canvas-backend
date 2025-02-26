package com.ygyin.coop.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 空间成员角色枚举类
 */
@Getter
public enum AreaRoleEnum {

    VIEWER("Viewer", "viewer"),
    EDITOR("Editor", "editor"),
    ADMIN("Admin", "admin");

    private final String text;

    private final String val;

    AreaRoleEnum(String text, String val) {
        this.text = text;
        this.val = val;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param val 枚举对象的值
     * @return 枚举对象
     */
    public static AreaRoleEnum getEnumByVal(String val) {
        if (ObjUtil.isEmpty(val))
            return null;

        for (AreaRoleEnum anEnum : AreaRoleEnum.values())
            if (anEnum.val.equals(val))
                return anEnum;

        return null;
    }

    /**
     * 获取所有枚举的文本列表
     *
     * @return 文本列表
     */
    public static List<String> getAllTexts() {
        return Arrays.stream(AreaRoleEnum.values())
                .map(AreaRoleEnum::getText)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有枚举的值列表
     *
     * @return 值列表
     */
    public static List<String> getAllVals() {
        return Arrays.stream(AreaRoleEnum.values())
                .map(AreaRoleEnum::getVal)
                .collect(Collectors.toList());
    }
}
