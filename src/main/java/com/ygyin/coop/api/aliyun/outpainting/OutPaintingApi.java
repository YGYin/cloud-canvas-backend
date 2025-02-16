package com.ygyin.coop.api.aliyun.outpainting;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;

import com.ygyin.coop.api.aliyun.model.GetOutPaintingTaskStatusResponse;
import com.ygyin.coop.api.aliyun.model.NewOutPaintingTaskRequest;
import com.ygyin.coop.api.aliyun.model.NewOutPaintingTaskResponse;
import com.ygyin.coop.exception.BusinessException;
import com.ygyin.coop.exception.ErrorCode;
import com.ygyin.coop.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 扩图 API
 * 1. 调用请求新建扩图任务
 * 2. 通过轮询获取扩图任务执行状态
 */
@Slf4j
@Component
public class OutPaintingApi {
    // 读取配置文件
    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    // 创建任务地址
    public static final String NEW_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    // 查询任务状态
    public static final String GET_OUT_PAINTING_TASK_STATUS_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    /**
     * 新建扩图任务
     *
     * @param newOutPaintingTaskRequest 新建扩图任务请求
     * @return
     */
    public NewOutPaintingTaskResponse createOutPaintingTask(NewOutPaintingTaskRequest newOutPaintingTaskRequest) {
        ThrowUtils.throwIf(newOutPaintingTaskRequest == null,
                ErrorCode.PARAMS_ERROR, "API: 新建扩图任务请求参数为空");

        // 发送请求
        HttpRequest request = HttpRequest.post(NEW_OUT_PAINTING_TASK_URL)
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                // 必须开启异步处理，设置为enable。
                .header("X-DashScope-Async", "enable")
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(JSONUtil.toJsonStr(newOutPaintingTaskRequest));
        try (HttpResponse httpResponse = request.execute()) {
            if (!httpResponse.isOk()) {
                log.error("请求异常：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "API: AI 扩图失败");
            }
            NewOutPaintingTaskResponse response = JSONUtil.toBean(httpResponse.body(), NewOutPaintingTaskResponse.class);
            String errorCode = response.getCode();
            if (StrUtil.isNotBlank(errorCode)) {
                String errorMessage = response.getMessage();
                log.error("AI 扩图失败，errorCode:{}, errorMessage:{}", errorCode, errorMessage);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "API: AI 扩图接口响应异常");
            }
            return response;
        }
    }

    /**
     * 查询扩图任务的状态
     *
     * @param taskId
     * @return
     */
    public GetOutPaintingTaskStatusResponse getOutPaintingTaskStatus(String taskId) {
        ThrowUtils.throwIf(taskId.isEmpty(),
                ErrorCode.OPERATION_ERROR, "API: 扩图任务 id 为空");

        try (HttpResponse response = HttpRequest.get(String.format(GET_OUT_PAINTING_TASK_STATUS_URL, taskId))
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                .execute()) {
            ThrowUtils.throwIf(!response.isOk(),
                    ErrorCode.OPERATION_ERROR, "API: 获取扩图任务状态失败");

            return JSONUtil.toBean(response.body(), GetOutPaintingTaskStatusResponse.class);
        }
    }
}


