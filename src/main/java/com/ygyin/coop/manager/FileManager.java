package com.ygyin.coop.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.ygyin.coop.config.CosClientConfig;
import com.ygyin.coop.exception.BusinessException;
import com.ygyin.coop.exception.ErrorCode;
import com.ygyin.coop.exception.ThrowUtils;
import com.ygyin.coop.model.dto.file.UploadImageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 通用文件上传服务，对 CosManager 的再封装
 * 转为使用 upload 中的模版代码
 */
@Slf4j
@Service
@Deprecated
public class FileManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    private static final long ONE_M = 1024 * 1024L;

    private static final List<String> ALLOW_UPLOAD_FORMAT = Arrays.asList("jpg", "jpeg", "png", "webp");

    private static final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp");

    /**
     * 上传图片
     *
     * @param multipartFile 文件
     * @param pathPrefix    上传路径的前缀
     * @return 上传图片的结果
     */
    public UploadImageResult uploadImage(MultipartFile multipartFile, String pathPrefix) {
        // 1. 对图片文件进行校验
        verifyImage(multipartFile);

        // 2. 拼接图片上传地址，
        // pathPrefix 为文件上传路径的前缀，文件名用 日期_uuid.原文件后缀名命名
        String uuid = RandomUtil.randomString(16);
        // 包含 文件名 . 后缀
        String originName = multipartFile.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originName);
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, suffix);
        // 实际上传路径为 文件上传路径的前缀 + 上传文件名
        String uploadPath = String.format("/%s/%s", pathPrefix, uploadFilename);

        // 3. 上传文件，可参考 File Controller
        File file = null;
        try {
            // 生成对应的缓存临时文件，需要将 multipartFile 转化储存为 file 类型
            file = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(file);
            // 上传图片，从中取出图片信息
            PutObjectResult putImgResult = cosManager.putImageObject(uploadPath, file);
            ImageInfo imgInfo = putImgResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            // 需要将上传结果返回给调用方，封装 UploadImageResult 包装类
            UploadImageResult uploadImgResult = new UploadImageResult();
            int imgWidth = imgInfo.getWidth();
            int imgHeight = imgInfo.getHeight();
            double imgScale = NumberUtil.round(imgWidth * 1.0 / imgHeight, 2).doubleValue();
            uploadImgResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            uploadImgResult.setName(FileUtil.mainName(originName));
            uploadImgResult.setImgSize(FileUtil.size(file));
            uploadImgResult.setImgWidth(imgWidth);
            uploadImgResult.setImgHeight(imgHeight);
            uploadImgResult.setImgScale(imgScale);
            uploadImgResult.setImgFormat(imgInfo.getFormat());

            return uploadImgResult;
        } catch (Exception e) {
            log.error("FileManager: 图片上传到 COS 失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "FileManager: 图片上传失败");
        } finally {
            deleteTempFile(file);
        }

    }

    /**
     * 通过文件 url 上传文件
     *
     * @param url 文件 url
     * @param pathPrefix 文件上传路径前缀
     * @return
     */
    public UploadImageResult uploadImageByUrl(String url, String pathPrefix) {
        // 1. 对图片文件 url 进行校验
        verifyImage(url);
        // 2. 拼接文件上传路径
        String uuid = RandomUtil.randomString(16);
        String originName = FileUtil.mainName(url);
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originName));
        String uploadPath = String.format("/%s/%s", pathPrefix, uploadFilename);

        File file = null;
        try {
            // 创建临时文件，根据图片文件 url 获取图片文件
            file = File.createTempFile(uploadPath, null);
            HttpUtil.downloadFile(url, file);
            // 上传图片
            // 上传图片，从中取出图片信息
            PutObjectResult putImgResult = cosManager.putImageObject(uploadPath, file);
            ImageInfo imgInfo = putImgResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            // 需要将上传结果返回给调用方，封装 UploadImageResult 包装类
            UploadImageResult uploadImgResult = new UploadImageResult();
            int imgWidth = imgInfo.getWidth();
            int imgHeight = imgInfo.getHeight();
            double imgScale = NumberUtil.round(imgWidth * 1.0 / imgHeight, 2).doubleValue();
            uploadImgResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            uploadImgResult.setName(FileUtil.mainName(originName));
            uploadImgResult.setImgSize(FileUtil.size(file));
            uploadImgResult.setImgWidth(imgWidth);
            uploadImgResult.setImgHeight(imgHeight);
            uploadImgResult.setImgScale(imgScale);
            uploadImgResult.setImgFormat(imgInfo.getFormat());

            return uploadImgResult;
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            this.deleteTempFile(file);
        }
    }

    private void deleteTempFile(File file) {
        // 判空
        if (file == null)
            return;
        boolean isDelete = file.delete();
        if (!isDelete)
            log.error("File Controller: 文件删除失败，filepath = {}", file.getAbsolutePath());
    }

    private void verifyImage(MultipartFile multipartFile) {
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

    private void verifyImage(String url) {
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
}
