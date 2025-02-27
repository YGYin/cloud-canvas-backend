package com.ygyin.coop.manager.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 客户端向服务端发送的图片编辑请求消息类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageEditRequestMessage {

    /**
     * 消息类型，例如 "START_EDIT", "END_EDIT", "EDIT_ACTION"
     */
    private String msgType;

    /**
     * 执行的编辑动作，例如缩放或者旋转
     */
    private String editAction;
}