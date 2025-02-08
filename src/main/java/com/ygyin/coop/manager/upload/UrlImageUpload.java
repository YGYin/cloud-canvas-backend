package com.ygyin.coop.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.ygyin.coop.exception.BusinessException;
import com.ygyin.coop.exception.ErrorCode;
import com.ygyin.coop.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * URL 类型图片上传
 */
@Service
public class UrlImageUpload extends ImageUploadTemplate {

    private static final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp");

    @Override
    protected void verifyImage(Object inputSource) {
        String url = (String) inputSource;
        // 1. 判空
        ThrowUtils.throwIf(StrUtil.isBlank(url), ErrorCode.PARAMS_ERROR, "文件地址不能为空");

        // 2. 验证 URL 格式，直接通过 URL 类来验证URL是否合法
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址格式不正确");
        }

        // 3. 校验 URL 是否为 http 或 https 协议
        ThrowUtils.throwIf(!(url.startsWith("http://") || url.startsWith("https://")),
                ErrorCode.PARAMS_ERROR, "仅支持 HTTP 或 HTTPS 协议的文件地址");

        // 4. 通过发送 HEAD 请求以验证文件是否存在，避免 GET 获取整个文件，
        // 但有可能服务器本身不支持 HEAD 请求，不能证明图片不存在
        HttpResponse response = null;
        try {
            response = HttpUtil.createRequest(Method.HEAD, url).execute();
            // 未正常返回，不抛异常
            if (response.getStatus() != HttpStatus.HTTP_OK)
                return;

            // 5. 通过响应的头信息获取校验图片 url 文件的类型
            String contentType = response.header("Content-Type");
            if (StrUtil.isNotBlank(contentType))
                // 允许的图片类型
                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPES.contains(contentType.toLowerCase()),
                        ErrorCode.PARAMS_ERROR, "Manager: 文件类型错误");

            // 6. 校验文件大小
            String contentLengthStr = response.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    final long TWO_MB = 2 * 1024 * 1024L; // 限制文件大小为 2MB
                    ThrowUtils.throwIf(contentLength > TWO_MB, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "Manager: 文件大小格式错误");
                }
            }
        } finally {
            if (response != null)
                response.close();
        }
    }

    @Override
    protected String getOriginalFilename(Object inputSource) {
        String url = (String) inputSource;
        return FileUtil.mainName(url);
    }

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        String url = (String) inputSource;
        HttpUtil.downloadFile(url, file);
    }
}
