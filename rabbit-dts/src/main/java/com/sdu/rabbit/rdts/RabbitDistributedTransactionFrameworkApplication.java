package com.sdu.rabbit.rdts;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("com.sdu")
@ComponentScan("com.sdu")
public class RabbitDistributedTransactionFrameworkApplication {

    public static void main(String[] args) {
        SpringApplication.run(RabbitDistributedTransactionFrameworkApplication.class, args);
    }
}
