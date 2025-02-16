package com.ygyin.coop.model.dto.image;

import com.ygyin.coop.api.aliyun.model.NewOutPaintingTaskRequest;
import lombok.Data;

import java.io.Serializable;

/**
 * 新建图片扩图任务请求类
 */
@Data
public class NewImageOutPaintingTaskRequest implements Serializable {
    /**
     * 图片 id
     */
    private Long imgId;

    /**
     * 扩图参数
     */
    private NewOutPaintingTaskRequest.Parameters parameters;

    private static final long serialVersionUID = 1L;

}