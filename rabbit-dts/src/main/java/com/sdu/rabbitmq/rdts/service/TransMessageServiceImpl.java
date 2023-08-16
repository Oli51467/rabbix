package com.sdu.rabbitmq.rdts.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.sdu.rabbitmq.common.annotation.RedissonLock;
import com.sdu.rabbitmq.rdts.domain.entity.TransMessage;
import com.sdu.rabbitmq.rdts.enums.TransMessageType;
import com.sdu.rabbitmq.rdts.repository.TransMessageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class TransMessageServiceImpl implements TransMessageService {

    @Resource
    private TransMessageMapper transMessageMapper;

    @Value("${rdts.service}")
    private String serviceName;

    @Override
    public TransMessage messageBeforeSend(String exchange, String routingKey, String body) {
        return saveMessage(exchange, routingKey, body, TransMessageType.SEND);
    }

    @Override
    public void sendMessageSuccess(String id) {
        QueryWrapper<TransMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id).ne("type", TransMessageType.LOST.toString());
        TransMessage transMessage = transMessageMapper.selectOne(queryWrapper);
        if (null != transMessage) {
            log.info("Deleted: {}", transMessage);
            deleteTransMessage(id);
        }
    }

    @Override
    public TransMessage handleMessageReturn(String id, String exchange, String routingKey, String body) {
        QueryWrapper<TransMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id);
        TransMessage transMessage = transMessageMapper.selectOne(queryWrapper);
        if (null == transMessage) {
            log.info("create lost message");
            return saveMessage(exchange, routingKey, body, TransMessageType.LOST);
        }
        transMessage.setType(TransMessageType.LOST);
        UpdateWrapper<TransMessage> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id);
        transMessageMapper.update(transMessage, updateWrapper);
        return transMessage;
    }

    @Override
    public List<TransMessage> getReadyMessages() {
        QueryWrapper<TransMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", TransMessageType.SEND.toString()).eq("service", serviceName);
        return transMessageMapper.selectList(queryWrapper);
    }

    @Override
    @RedissonLock(prefixKey = "rabbit-dts", key = "#id", waitTime = 5000)
    public void resendMessage(String id) {
        UpdateWrapper<TransMessage> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id).eq("service", serviceName)
                .setSql("`sequence` = `sequence` + 1");
        transMessageMapper.update(null, updateWrapper);
    }

    @Override
    public void handleMessageDead(String id) {
        UpdateWrapper<TransMessage> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id).eq("service", serviceName).set("type", TransMessageType.DEAD);
        transMessageMapper.update(null, updateWrapper);
    }

    @Override
    public void handleMessageDead(String id, String exchange, String routingKey, String queue, String body) {
        TransMessage transMessage = new TransMessage();
        transMessage.setId(id);
        transMessage.setService(serviceName);
        transMessage.setExchange(exchange);
        transMessage.setRoutingKey(routingKey);
        transMessage.setQueue(queue);
        transMessage.setPayload(body);
        transMessage.setSequence(0);
        transMessage.setType(TransMessageType.DEAD);
        transMessage.setCreateTime(new Date());
        transMessageMapper.insert(transMessage);
    }

    @Override
    public TransMessage messageBeforeConsume(String id, String exchange, String routingKey, String queue, String body) {
        // 查询数据库中有无该消息，若有，增加消费次数，若没有则新建
        QueryWrapper<TransMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id).eq("service", serviceName);
        TransMessage transMessage = transMessageMapper.selectOne(queryWrapper);
        // 没有则新建
        if (null == transMessage) {
            transMessage = new TransMessage();
            transMessage.setId(id);
            transMessage.setService(serviceName);
            transMessage.setExchange(exchange);
            transMessage.setRoutingKey(routingKey);
            transMessage.setQueue(queue);
            transMessage.setPayload(body);
            transMessage.setSequence(0);
            transMessage.setType(TransMessageType.RECEIVE);
            transMessage.setCreateTime(new Date());
            transMessageMapper.insert(transMessage);
        } else {
            UpdateWrapper<TransMessage> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", transMessage.getId());
            transMessage.setSequence(transMessage.getSequence() + 1);
            transMessageMapper.update(transMessage, updateWrapper);
        }
        return transMessage;
    }

    @Override
    public void consumeMessageSuccess(String id) {
        deleteTransMessage(id);
    }

    /**
     * 更改该消息的状态为失败
     * @param id 消息id
     */
    @Override
    public void consumeMessageFailed(String id) {
        QueryWrapper<TransMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id).eq("service", serviceName);
        TransMessage transMessage = transMessageMapper.selectOne(queryWrapper);
        if (null != transMessage) {
            UpdateWrapper<TransMessage> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", id).set("type", TransMessageType.ERROR.toString());
            transMessageMapper.update(null, updateWrapper);
        }
    }

    private TransMessage saveMessage(String exchange, String routingKey, String body, TransMessageType type) {
        TransMessage transMessage = new TransMessage();
        transMessage.setId(UUID.randomUUID().toString().replace("-", ""));
        transMessage.setService(serviceName);
        transMessage.setExchange(exchange);
        transMessage.setRoutingKey(routingKey);
        transMessage.setPayload(body);
        transMessage.setSequence(0);
        transMessage.setCreateTime(new Date());
        transMessage.setType(type);
        transMessageMapper.insert(transMessage);
        return transMessage;
    }

    private void saveMessage(String id, String exchange, String queue, String routingKey, String body, TransMessageType type) {
        TransMessage transMessage = new TransMessage();
        transMessage.setId(id);
        transMessage.setService(serviceName);
        transMessage.setExchange(exchange);
        transMessage.setRoutingKey(routingKey);
        transMessage.setQueue(queue);
        transMessage.setPayload(body);
        transMessage.setSequence(0);
        transMessage.setType(type);
        transMessage.setCreateTime(new Date());
        transMessageMapper.insert(transMessage);
    }

    private void deleteTransMessage(String id) {
        QueryWrapper<TransMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id).eq("service", serviceName);
        transMessageMapper.delete(queryWrapper);
    }
}
