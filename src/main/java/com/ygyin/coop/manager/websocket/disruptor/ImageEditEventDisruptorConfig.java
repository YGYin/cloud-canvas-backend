package com.ygyin.coop.manager.websocket.disruptor;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.lmax.disruptor.dsl.Disruptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * Disruptor 配置，定义缓存队列大小。绑定消费者
 */
@Configuration
public class ImageEditEventDisruptorConfig {

    /**
     * 事件处理器(消费者)
     */
    @Resource
    private ImageEditEventWorkHandler imgEditEventWorkHandler;

    /**
     * 将事件类型，缓存队列大小关联到 Disruptor 实例中
     *
     * @return
     */
    @Bean("imgEditEventDisruptor")
    public Disruptor<ImageEditEvent> messageModelRingBuffer() {
        // 设置 ringBuffer 的大小
        int bufferSize = 1024 * 256;
        // 设置 disruptor 事件类型，缓存大小，定义线程池
        Disruptor<ImageEditEvent> disruptor = new Disruptor<>(
                ImageEditEvent::new,
                bufferSize,
                ThreadFactoryBuilder.create().setNamePrefix("imgEditEventDisruptor").build()
        );
        // 绑定消费者，并启动 disruptor
        disruptor.handleEventsWithWorkerPool(imgEditEventWorkHandler);
        disruptor.start();
        return disruptor;
    }
}
