package com.sdu.rabbitmq.reward;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan({"com.sdu.rabbitmq"})
@ComponentScan("com.sdu.rabbitmq")
@EnableAsync
public class RewardServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RewardServiceApplication.class, args);
    }
}