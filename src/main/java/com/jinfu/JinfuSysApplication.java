package com.jinfu;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.jinfu.**.mapper")
@EnableAsync
public class JinfuSysApplication {

    public static void main(String[] args) {
        SpringApplication.run(JinfuSysApplication.class, args);
    }
}
