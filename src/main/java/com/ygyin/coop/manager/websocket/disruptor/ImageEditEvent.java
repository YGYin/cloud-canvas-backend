package com.ygyin.coop.manager.websocket.disruptor;


import com.ygyin.coop.manager.websocket.model.ImageEditRequestMessage;
import com.ygyin.coop.model.entity.User;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

/**
 * 定义 Disruptor 事件，用于做上下文容器，封装处理消息所需数据
 */
@Data
public class ImageEditEvent {

    /**
     * 消息
     */
    private ImageEditRequestMessage imgEditRequestMessage;

    /**
     * 当前用户的 session
     */
    private WebSocketSession session;

    /**
     * 当前用户
     */
    private User user;

    /**
     * 图片 id
     */
    private Long imgId;

}
