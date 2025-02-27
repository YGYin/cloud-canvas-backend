package com.ygyin.coop.manager.websocket.model;

import com.ygyin.coop.model.vo.UserVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务端向客户端发送的图片编辑响应消息类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageEditResponseMessage {

    /**
     * 消息类型，例如 "START_EDIT", "END_EDIT", "EDIT_ACTION", "INFO"
     */
    private String msgType;

    /**
     * 响应消息内容
     */
    private String message;

    /**
     * 执行的编辑动作，例如缩放或者旋转
     */
    private String editAction;

    /**
     * 发起事件的用户
     */
    private UserVO user;
}