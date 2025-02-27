package com.ygyin.coop.manager.websocket.disruptor;

import cn.hutool.json.JSONUtil;
import com.lmax.disruptor.WorkHandler;
import com.ygyin.coop.manager.websocket.ImageEditHandler;
import com.ygyin.coop.manager.websocket.model.ImageEditMessageTypeEnum;
import com.ygyin.coop.manager.websocket.model.ImageEditRequestMessage;
import com.ygyin.coop.manager.websocket.model.ImageEditResponseMessage;
import com.ygyin.coop.model.entity.User;
import com.ygyin.coop.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;

/**
 * Disruptor 事件处理器，从当消费者角色，将不同类型的消息分发到对应处理器中
 */
@Slf4j
@Component
public class ImageEditEventWorkHandler implements WorkHandler<ImageEditEvent> {

    @Resource
    private ImageEditHandler imgEditHandler;

    @Resource
    private UserService userService;

    @Override
    public void onEvent(ImageEditEvent event) throws Exception {
        ImageEditRequestMessage imgEditRequestMessage = event.getImgEditRequestMessage();
        WebSocketSession session = event.getSession();
        User user = event.getUser();
        Long imgId = event.getImgId();

        // 获取到消息类别
        String type = imgEditRequestMessage.getMsgType();
        ImageEditMessageTypeEnum imgEditMessageTypeEnum = ImageEditMessageTypeEnum.valueOf(type);

        // 调用对应的消息处理方法
        switch (imgEditMessageTypeEnum) {
            case START_EDIT:
                imgEditHandler.handleStartEditMessage(imgEditRequestMessage, session, user, imgId);
                break;
            case EDIT_ACTION:
                imgEditHandler.handleEditActionMessage(imgEditRequestMessage, session, user, imgId);
                break;
            case END_EDIT:
                imgEditHandler.handleEndEditMessage(imgEditRequestMessage, session, user, imgId);
                break;
            default:
                // 默认构造响应，返回错误提示给当前 session 用户
                ImageEditResponseMessage imgEditResponseMessage = new ImageEditResponseMessage();
                imgEditResponseMessage.setMsgType(ImageEditMessageTypeEnum.ERROR.getVal());
                imgEditResponseMessage.setMessage("消息类型错误");
                imgEditResponseMessage.setUser(userService.getUserVO(user));
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(imgEditResponseMessage)));
        }
    }
}
