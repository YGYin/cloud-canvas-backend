package com.ygyin.coop.manager.upload;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.ygyin.coop.config.CosClientConfig;
import com.ygyin.coop.exception.BusinessException;
import com.ygyin.coop.exception.ErrorCode;
import com.ygyin.coop.manager.CosManager;
import com.ygyin.coop.model.dto.file.UploadImageResult;
import lombok.extern.slf4j.Slf4j;


import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 图片文件上传模板
 */
@Slf4j
public abstract class ImageUploadTemplate {

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
     * @param inputSource 文件
     * @param pathPrefix  上传路径的前缀
     * @return 上传图片的结果
     */
    public UploadImageResult uploadImage(Object inputSource, String pathPrefix) {
        // 1. 对图片文件进行校验
        verifyImage(inputSource);

        // 2. 拼接图片上传地址，
        // pathPrefix 为文件上传路径的前缀，文件名用 日期_uuid.原文件后缀名命名
        String uuid = RandomUtil.randomString(16);
        // 包含 文件名 . 后缀
        String originName = getOriginalFilename(inputSource);
        String suffix = FileUtil.getSuffix(originName);
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, suffix);
        // 实际上传路径为 文件上传路径的前缀 + 上传文件名
        String uploadPath = String.format("/%s/%s", pathPrefix, uploadFilename);

        // 3. 上传文件，可参考 File Controller
        File file = null;
        try {
            // 生成对应的缓存临时文件，需要将 multipartFile 转化储存为 file 类型
            file = File.createTempFile(uploadPath, null);
            processFile(inputSource, file);
            // 4. 上传图片到 COS
            PutObjectResult putImgResult = cosManager.putImageObject(uploadPath, file);
            ImageInfo imgInfo = putImgResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            // 5. 从中取出图片信息，封装返回结果
            return buildUploadResult(imgInfo, uploadPath, originName, file);
        } catch (Exception e) {
            log.error("FileManager: 图片上传到 COS 失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "FileManager: 图片上传失败");
        } finally {
            //6. 清理临时文件
            deleteTempFile(file);
        }

    }

    /**
     * 封装返回结果
     *
     * @param imgInfo
     * @param uploadPath
     * @param originName
     * @param file
     * @return
     */
    private UploadImageResult buildUploadResult(ImageInfo imgInfo, String uploadPath, String originName, File file) {
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
    }

    /**
     * 将输入源生成本地临时文件
     * @param inputSource
     * @param file
     */
    protected abstract void processFile(Object inputSource, File file) throws Exception;

    /**
     * 校验输入源，本地图片文件或文件 url
     *
     * @param inputSource
     */
    protected abstract void verifyImage(Object inputSource);

    /**
     * 回去输入源的原始文件名
     *
     * @param inputSource
     * @return
     */
    protected abstract String getOriginalFilename(Object inputSource);

    private void deleteTempFile(File file) {
        // 判空
        if (file == null)
            return;
        boolean isDelete = file.delete();
        if (!isDelete)
            log.error("File Controller: 文件删除失败，filepath = {}", file.getAbsolutePath());
    }


}