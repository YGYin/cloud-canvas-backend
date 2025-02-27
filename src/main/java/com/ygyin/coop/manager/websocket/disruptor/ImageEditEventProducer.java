package com.ygyin.coop.manager.websocket.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.ygyin.coop.manager.websocket.model.ImageEditRequestMessage;
import com.ygyin.coop.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * Disruptor 生产者，负责生产发布消息到队列中
 */
@Component
@Slf4j
public class ImageEditEventProducer {

    @Resource
    Disruptor<ImageEditEvent> imgEditEventDisruptor;

    public void publishEvent(ImageEditRequestMessage imgEditRequestMessage, WebSocketSession session, User user, Long imgId) {
        // 获取到环形缓存队列
        RingBuffer<ImageEditEvent> ringBuffer = imgEditEventDisruptor.getRingBuffer();

        // 获取环形缓存队列当前可以放置发布事件的位置，获取到空事件对象
        long next = ringBuffer.next();
        ImageEditEvent imgEditEvent = ringBuffer.get(next);
        // 填充事件对象
        imgEditEvent.setSession(session);
        imgEditEvent.setImgEditRequestMessage(imgEditRequestMessage);
        imgEditEvent.setUser(user);
        imgEditEvent.setImgId(imgId);
        // 发布事件
        ringBuffer.publish(next);
    }

    /**
     * 实现优雅停机，Disruptor 处理完所有事件才会关闭
     */
    @PreDestroy
    public void close() {
        imgEditEventDisruptor.shutdown();
    }
}
