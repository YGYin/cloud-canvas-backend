package com.ygyin.coop.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.ygyin.coop.exception.ErrorCode;
import com.ygyin.coop.exception.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * 文件类型图片上传
 */
@Service
public class FileImageUpload extends ImageUploadTemplate {

    private static final long ONE_M = 1024 * 1024L;

    private static final List<String> ALLOW_UPLOAD_FORMAT = Arrays.asList("jpg", "jpeg", "png", "webp");

    @Override
    protected void verifyImage(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        // 对文件判空
        ThrowUtils.throwIf(multipartFile == null,
                ErrorCode.PARAMS_ERROR, "上传文件不能为空");

        // 1. 校验上传文件大小，不超过 2M
        long size = multipartFile.getSize();
        ThrowUtils.throwIf(size > (ONE_M * 2),
                ErrorCode.PARAMS_ERROR, "上传文件大小不能超过 2M");

        // 2. 校验上传文件后缀是否合法
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        ThrowUtils.throwIf(!ALLOW_UPLOAD_FORMAT.contains(suffix),
                ErrorCode.PARAMS_ERROR, "上传文件类型不支持");
    }

    @Override
    protected String getOriginalFilename(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        multipartFile.transferTo(file);
    }
}
