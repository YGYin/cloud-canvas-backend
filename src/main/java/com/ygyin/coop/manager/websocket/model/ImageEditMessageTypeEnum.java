package com.ygyin.coop.manager.websocket.model;

import lombok.Getter;

/**
 * 图片编辑消息类型枚举类
 */
@Getter
public enum ImageEditMessageTypeEnum {

    INFO("发送通知", "INFO"),
    ERROR("发送错误", "ERROR"),
    START_EDIT("进入编辑状态", "START_EDIT"),
    END_EDIT("退出编辑状态", "END_EDIT"),
    EDIT_ACTION("执行编辑操作", "EDIT_ACTION");

    private final String text;
    private final String val;

    ImageEditMessageTypeEnum(String text, String val) {
        this.text = text;
        this.val = val;
    }

    /**
     * 根据 val 获取枚举
     */
    public static ImageEditMessageTypeEnum getEnumByVal(String val) {
        if (val == null || val.isEmpty())
            return null;

        for (ImageEditMessageTypeEnum typeEnum : ImageEditMessageTypeEnum.values())
            if (typeEnum.val.equals(val))
                return typeEnum;

        return null;
    }
}
