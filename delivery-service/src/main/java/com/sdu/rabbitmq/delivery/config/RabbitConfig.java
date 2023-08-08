package com.sdu.rabbitmq.delivery.config;

import com.sdu.rabbitmq.delivery.service.OrderMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Slf4j
@Configuration
public class RabbitConfig {

    @Autowired
    private OrderMessageService orderMessageService;

    @Autowired
    public void startListenMessage() throws IOException, TimeoutException {
        orderMessageService.handleMessage();
    }
}
