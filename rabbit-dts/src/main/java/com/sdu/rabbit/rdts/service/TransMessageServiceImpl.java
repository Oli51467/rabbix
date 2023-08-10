package com.sdu.rabbit.rdts.service;

import com.sdu.rabbit.rdts.domain.TransMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class TransMessageServiceImpl implements TransMessageService {

    @Override
    public TransMessage saveMessageBeforeSend(String exchange, String routingKey, String body) {
        return null;
    }

    @Override
    public void sendMessageSuccess(String id) {

    }

    @Override
    public TransMessage handleMessageReturn(String id, String exchange, String routingKey, String body) {
        return null;
    }

    @Override
    public List<TransMessage> getReadyMessages() {
        return null;
    }

    @Override
    public void resendMessage(String id) {

    }

    @Override
    public void messageDead(String id) {

    }

    @Override
    public TransMessage saveMessageBeforeConsume(String id, String exchange, String routingKey, String queue, String body) {
        return null;
    }

    @Override
    public void consumeMessageSuccess(String id) {

    }
}
