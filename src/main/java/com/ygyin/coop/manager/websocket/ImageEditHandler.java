package com.ygyin.coop.manager.websocket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ygyin.coop.manager.websocket.model.ImageEditActionEnum;
import com.ygyin.coop.manager.websocket.model.ImageEditMessageTypeEnum;
import com.ygyin.coop.manager.websocket.model.ImageEditRequestMessage;
import com.ygyin.coop.manager.websocket.model.ImageEditResponseMessage;
import com.ygyin.coop.model.entity.User;
import com.ygyin.coop.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ImageEditHandler extends TextWebSocketHandler {

    @Resource
    private UserService userService;

    /**
     * 每张图片的编辑状态，key: imgId, value: 当前正在编辑该图片的用户 id
     */
    private final Map<Long, Long> imgEditingUsers = new ConcurrentHashMap<>();

    /**
     * 当前图片建立连接的所有会话，key: imgId, value: 用户的 session 集合
     */
    private final Map<Long, Set<WebSocketSession>> imgSessions = new ConcurrentHashMap<>();


    /**
     * 当前用户建立连接后维护 sessions map，构造响应返回给所有用户
     *
     * @param session 当前用户 session
     * @throws Exception
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从 session 中取得 imgId，保存 session 到建立连接的所有会话 map 中
        Long imgId = (Long) session.getAttributes().get("imgId");
        imgSessions.putIfAbsent(imgId, ConcurrentHashMap.newKeySet());
        imgSessions.get(imgId).add(session);

        // 构造图片编辑响应消息
        ImageEditResponseMessage imgEditResponseMessage = new ImageEditResponseMessage();
        imgEditResponseMessage.setMsgType(ImageEditMessageTypeEnum.INFO.getVal());
        User user = (User) session.getAttributes().get("user");
        String message = String.format("%s加入编辑", user.getUserName());
        imgEditResponseMessage.setMessage(message);
        imgEditResponseMessage.setUser(userService.getUserVO(user));
        // 广播给同一张图片的用户
        broadcastToUserInImg(imgId, imgEditResponseMessage);
    }


    /**
     * 收到前端或客户端发送图片编辑的操作消息后，根据消息类型作处理
     *
     * @param session
     * @param message
     * @throws Exception
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        // 将前端发来的操作消息解析为 ImageEditMessage，并获取消息类型及枚举类，用于做下一步处理
        ImageEditRequestMessage imgEditRequestMessage = JSONUtil.toBean(message.getPayload(), ImageEditRequestMessage.class);
        String msgType = imgEditRequestMessage.getMsgType();
        ImageEditMessageTypeEnum imgEditMessageTypeEnum = ImageEditMessageTypeEnum.valueOf(msgType);

        // 从 Session 属性中获取公共参数
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long imgId = (Long) attributes.get("imgId");

        // 调用对应的消息处理方法
        switch (imgEditMessageTypeEnum) {
            case START_EDIT:
                handleStartEditMessage(imgEditRequestMessage, session, user, imgId);
                break;
            case EDIT_ACTION:
                handleEditActionMessage(imgEditRequestMessage, session, user, imgId);
                break;
            case END_EDIT:
                handleEndEditMessage(imgEditRequestMessage, session, user, imgId);
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

    /**
     * 进入编辑状态处理
     *
     * @param imgEditRequestMessage
     * @param session
     * @param user
     * @param imgId
     * @throws Exception
     */
    public void handleStartEditMessage(ImageEditRequestMessage imgEditRequestMessage, WebSocketSession session, User user, Long imgId) throws Exception {
        // 没有用户正在编辑该图片，才能进入编辑
        // todo: 可能导致存在多个用户编辑
        if (!imgEditingUsers.containsKey(imgId)) {
            // 设置当前用户为编辑用户
            imgEditingUsers.put(imgId, user.getId());
            // 构造响应
            ImageEditResponseMessage imgEditResponseMessage = new ImageEditResponseMessage();
            imgEditResponseMessage.setMsgType(ImageEditMessageTypeEnum.START_EDIT.getVal());
            String message = String.format("%s开始编辑图片", user.getUserName());
            imgEditResponseMessage.setMessage(message);
            imgEditResponseMessage.setUser(userService.getUserVO(user));
            // 广播给所有用户
            broadcastToUserInImg(imgId, imgEditResponseMessage);
        }
    }

    /**
     * 编辑操作处理
     *
     * @param imgEditRequestMessage
     * @param session
     * @param user
     * @param imgId
     * @throws Exception
     */
    public void handleEditActionMessage(ImageEditRequestMessage imgEditRequestMessage, WebSocketSession session, User user, Long imgId) throws Exception {
        // 参数中获取编辑操作，看编辑操作类型是否合法
        String editAction = imgEditRequestMessage.getEditAction();
        ImageEditActionEnum actionEnum = ImageEditActionEnum.getEnumByVal(editAction);
        if (actionEnum == null)
            return;

        // 校验发送编辑操作信息的用户和当前 session 用户是否一致，一致则为当前编辑者
        Long editingUserId = imgEditingUsers.get(imgId);
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            // 构造响应
            ImageEditResponseMessage imgEditResponseMessage = new ImageEditResponseMessage();
            imgEditResponseMessage.setMsgType(ImageEditMessageTypeEnum.EDIT_ACTION.getVal());
            String message = String.format("%s对图片执行%s操作", user.getUserName(), actionEnum.getText());
            imgEditResponseMessage.setMessage(message);
            imgEditResponseMessage.setEditAction(editAction);
            imgEditResponseMessage.setUser(userService.getUserVO(user));
            // 广播给除了当前客户端之外的其他用户，否则会造成重复编辑
            broadcastToUserInImg(imgId, imgEditResponseMessage, session);
        }
    }

    /**
     * 结束编辑状态处理
     *
     * @param imgEditRequestMessage
     * @param session
     * @param user
     * @param imgId
     * @throws Exception
     */
    public void handleEndEditMessage(ImageEditRequestMessage imgEditRequestMessage, WebSocketSession session, User user, Long imgId) throws Exception {
        // 获取结束编辑请求用户 id，校验发送结束编辑操作的用户和当前 session 用户是否一致，一致则为当前编辑者
        Long editingUserId = imgEditingUsers.get(imgId);
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            // 移除当前用户的编辑状态
            imgEditingUsers.remove(imgId);
            // 构造响应，发送结束编辑的消息通知
            ImageEditResponseMessage imgEditResponseMessage = new ImageEditResponseMessage();
            imgEditResponseMessage.setMsgType(ImageEditMessageTypeEnum.END_EDIT.getVal());
            String message = String.format("%s结束编辑图片", user.getUserName());
            imgEditResponseMessage.setMessage(message);
            imgEditResponseMessage.setUser(userService.getUserVO(user));
            broadcastToUserInImg(imgId, imgEditResponseMessage);
        }
    }


    /**
     * WebSocket 关闭连接后，移除当前用户编辑状态，从 map 中删除当前会话 session，通知其他客户端
     *
     * @param session
     * @param status
     * @throws Exception
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Map<String, Object> attributes = session.getAttributes();
        Long imgId = (Long) attributes.get("imgId");
        User user = (User) attributes.get("user");
        // 移除当前用户的编辑状态
        handleEndEditMessage(null, session, user, imgId);

        // 从当前图片建立的所有会话集合中中删除当前会话 session
        Set<WebSocketSession> sessionSet = imgSessions.get(imgId);
        if (sessionSet != null) {
            sessionSet.remove(session);
            // 如果当前图片已经没有用户建立会话，移除 imgSessions 中当前图片 id 的 k-v
            if (sessionSet.isEmpty())
                imgSessions.remove(imgId);
        }

        // 构造响应，通知所有用户 WebSocket 连接已关闭
        ImageEditResponseMessage imgEditResponseMessage = new ImageEditResponseMessage();
        imgEditResponseMessage.setMsgType(ImageEditMessageTypeEnum.INFO.getVal());
        String message = String.format("%s离开编辑", user.getUserName());
        imgEditResponseMessage.setMessage(message);
        imgEditResponseMessage.setUser(userService.getUserVO(user));
        broadcastToUserInImg(imgId, imgEditResponseMessage);
    }


    /**
     * 将消息广播给建立连接的所有用户，可排除特定 session
     *
     * @param imgId
     * @param imgEditResponseMessage
     * @param excludeSession
     * @throws Exception
     */
    private void broadcastToUserInImg(Long imgId, ImageEditResponseMessage imgEditResponseMessage, WebSocketSession excludeSession) throws Exception {
        Set<WebSocketSession> sessionSet = imgSessions.get(imgId);
        if (CollUtil.isNotEmpty(sessionSet)) {
            // 配置序列化，将 Long 类型转为 String，解决丢失精度问题
            ObjectMapper objectMapper = new ObjectMapper();
            SimpleModule module = new SimpleModule();
            // 支持 Long 以及 long 基本类型
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);
            objectMapper.registerModule(module);
            // 序列化为 JSON 字符串
            String message = objectMapper.writeValueAsString(imgEditResponseMessage);
            TextMessage textMessage = new TextMessage(message);
            for (WebSocketSession session : sessionSet) {
                // 如果刚好是当前进行操作发出通知的用户 session 不发送
                if (excludeSession != null && excludeSession.equals(session))
                    continue;
                // 发送给其他建立了连接的用户
                if (session.isOpen())
                    session.sendMessage(textMessage);
            }
        }
    }

    /**
     * 广播方法重载，广播给所有建立连接的用户，不排除特定 session
     *
     * @param imgId
     * @param imgEditResponseMessage
     * @throws Exception
     */
    private void broadcastToUserInImg(Long imgId, ImageEditResponseMessage imgEditResponseMessage) throws Exception {
        broadcastToUserInImg(imgId, imgEditResponseMessage, null);
    }

}
