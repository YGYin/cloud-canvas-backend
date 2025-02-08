package com.ygyin.coop.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
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
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 通用文件上传服务，对 CosManager 的再封装
 */
@Slf4j
@Service
public class FileManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    private static final long ONE_M = 1024 * 1024L;

    private static final List<String> ALLOW_UPLOAD_FORMAT = Arrays.asList("jpg", "jpeg", "png", "webp");

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
        String filename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, suffix);
        // 实际上传路径为 文件上传路径的前缀 + 上传文件名
        String uploadPath = String.format("/%s/%s", pathPrefix, filename);

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

}
