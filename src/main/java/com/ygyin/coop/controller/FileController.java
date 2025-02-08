package com.ygyin.coop.controller;

import com.ygyin.coop.annotation.AuthVerify;
import com.ygyin.coop.common.BaseResponse;
import com.ygyin.coop.common.ResUtils;
import com.ygyin.coop.constant.UserConstant;
import com.ygyin.coop.exception.BusinessException;
import com.ygyin.coop.exception.ErrorCode;
import com.ygyin.coop.manager.CosManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;

@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    @Resource
    private CosManager cosManager;

    /**
     * 测试文件上传
     *
     * @param multipartFile 上传文件 Form-data 表单
     * @return 上传成功后的文件路径
     */
    @AuthVerify(requiredRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> uploadFileTest(@RequestPart("file") MultipartFile multipartFile) {
        // 获取文件名和路径
        String filename = multipartFile.getOriginalFilename();
        String filePath = String.format("/test/%s", filename);
        // 新建文件用于上传
        File file = null;
        try {
            // 生成对应的缓存文件，需要将 multipartFile 转化储存为 file 类型
            file = File.createTempFile(filePath, "");
            multipartFile.transferTo(file);
            // 上传文件
            cosManager.putObject(filePath, file);
            // 返回可访问的文件地址
            return ResUtils.success(filePath);
        } catch (Exception e) {
            // 打印日志，抛业务异常
            log.error("File Controller: 文件上传失败，filepath = {}", filePath);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "File Controller: 文件上传失败");
        } finally {
            if (file != null) {
                // 删除临时文件
                boolean isDelete = file.delete();
                if (!isDelete)
                    log.error("File Controller: 文件删除失败，filepath = {}", filePath);
            }
        }
    }
}
