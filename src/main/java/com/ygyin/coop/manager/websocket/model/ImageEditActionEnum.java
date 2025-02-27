package com.ygyin.coop.manager.websocket.model;

import lombok.Getter;

/**
 * 图片编辑操作枚举类
 */
@Getter
public enum ImageEditActionEnum {

    ZOOM_IN("放大", "ZOOM_IN"),
    ZOOM_OUT("缩小", "ZOOM_OUT"),
    LEFT_ROTATION("左旋", "LEFT_ROTATION"),
    RIGHT_ROTATION("右旋", "RIGHT_ROTATION");

    private final String text;
    private final String val;

    ImageEditActionEnum(String text, String val) {
        this.text = text;
        this.val = val;
    }

    /**
     * 根据 val 获取枚举
     */
    public static ImageEditActionEnum getEnumByVal(String val) {
        if (val == null || val.isEmpty())
            return null;

        for (ImageEditActionEnum actionEnum : ImageEditActionEnum.values())
            if (actionEnum.val.equals(val))
                return actionEnum;

        return null;
    }
}
