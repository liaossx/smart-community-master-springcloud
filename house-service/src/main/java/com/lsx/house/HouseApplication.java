package com.lsx.house;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.lsx.house", "com.lsx.core.common"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.lsx.house.client", "com.lsx.core.common.client"})
@MapperScan("com.lsx.house.mapper")
public class HouseApplication {
    public static void main(String[] args) {
        SpringApplication.run(HouseApplication.class, args);
    }
}
