package com.ygyin.coop.manager;

import cn.hutool.core.io.FileUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.ygyin.coop.config.CosClientConfig;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 通用 COS 对象储存文件操作
 */
@Component
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传文件(对象)
     *
     * @param key
     * @param file
     * @return
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest request = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(request);
    }

    /**
     * 上传图片文件(对象)，附带图片的信息
     *
     * @param key
     * @param file
     * @return
     */
    public PutObjectResult putImageObject(String key, File file) {
        PutObjectRequest request = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        // 通过设置 PicOp 获取图片信息，设置为 1 表示返回该图片的信息
        PicOperations picOperations = new PicOperations();
        picOperations.setIsPicInfo(1);

        List<PicOperations.Rule> ruleList = new ArrayList<>();
        // 1. 设定图片处理规则，将上传图片转为 webp 格式
        String webpKey = FileUtil.mainName(key) + ".webp";
        PicOperations.Rule compress = new PicOperations.Rule();
        compress.setFileId(webpKey);
        compress.setBucket(cosClientConfig.getBucket());
        compress.setRule("imageMogr2/format/webp");
        ruleList.add(compress);

        // 2. 将对大小超过 30kb 的图片处理为缩略图
        if (file.length() > 3 * 1024) {
            PicOperations.Rule thumb = new PicOperations.Rule();
            // 约定缩略图路径为默认文件名加上 _thumb
            String thumbKey = FileUtil.mainName(key) + "_thumb" + FileUtil.getSuffix(key);
            thumb.setFileId(thumbKey);
            thumb.setBucket(cosClientConfig.getBucket());
            thumb.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 256, 256));
            ruleList.add(thumb);
        }

        picOperations.setRules(ruleList);
        // 将图片信息一同封装到 request 中
        request.setPicOperations(picOperations);
        return cosClient.putObject(request);
    }


    /**
     * 下载文件(对象)
     *
     * @param key
     * @return
     */
    public COSObject getObject(String key) {
        GetObjectRequest request = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(request);
    }

    /**
     * 删除对象
     *
     * @param key 文件 key
     */
    public void deleteObject(String key) throws CosClientException {
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }

}
