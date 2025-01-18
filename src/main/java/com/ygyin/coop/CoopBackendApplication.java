package com.ygyin.coop;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan("com.ygyin.coop.mapper")
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class CoopBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoopBackendApplication.class, args);
    }

}
