package com.ygyin.coop.controller;

import com.ygyin.coop.common.BaseResponse;
import com.ygyin.coop.common.ResUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class MainController {

    /**
     * 健康检查
     * @return
     */
    @GetMapping("/health")
    public BaseResponse<String> health() {
        return ResUtils.success("Healthy");
    }
}
