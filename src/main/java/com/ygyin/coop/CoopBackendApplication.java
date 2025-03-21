package com.ygyin.coop;

import org.apache.shardingsphere.spring.boot.ShardingSphereAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = {ShardingSphereAutoConfiguration.class})
@MapperScan("com.ygyin.coop.mapper")
@EnableAsync
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class CoopBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoopBackendApplication.class, args);
    }

}
