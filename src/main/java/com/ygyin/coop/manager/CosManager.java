package com.ygyin.coop.manager;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.ygyin.coop.config.CosClientConfig;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;

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
     * @param key
     * @param file
     * @return
     */
    public PutObjectResult putImageObject(String key, File file) {
        PutObjectRequest request = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        // 通过设置 PicOp 获取图片信息，设置为 1 表示返回该图片的信息
        PicOperations picOperations = new PicOperations();
        picOperations.setIsPicInfo(1);

        // 将图片信息一同封装到 request 中
        request.setPicOperations(picOperations);
        return cosClient.putObject(request);
    }


    /**
     * 下载文件(对象)
     * @param key
     * @return
     */
    public COSObject getObject(String key) {
        GetObjectRequest request = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(request);
    }
}
