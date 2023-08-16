package com.sdu.rabbitmq.order.listener;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdu.rabbitmq.order.entity.dto.OrderMessageDTO;
import com.sdu.rabbitmq.order.entity.po.OrderDetail;
import com.sdu.rabbitmq.order.enums.OrderStatus;
import com.sdu.rabbitmq.order.mapper.ProductMapper;
import com.sdu.rabbitmq.order.repository.OrderDetailMapper;
import com.sdu.rabbitmq.rdts.listener.AbstractDlxListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

@Component
@Slf4j
public class DlxListener extends AbstractDlxListener {

    @Resource
    private OrderDetailMapper orderDetailMapper;

    @Resource
    private ProductMapper productMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean receiveMessage(Message message) throws IOException {
        String messageBody = new String(message.getBody());
        log.info("接收到的消息体: {}", messageBody);
        OrderMessageDTO orderMessage = objectMapper.readValue(message.getBody(), OrderMessageDTO.class);
        log.info("当前订单状态: {}", orderMessage.getOrderStatus());
        if (orderMessage.getOrderStatus().equals(OrderStatus.WAITING_PAY)) {
            // 将该订单关闭
            UpdateWrapper<OrderDetail> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", orderMessage.getOrderId()).set("status", OrderStatus.FAILED.toString());
            orderDetailMapper.update(null, updateWrapper);
            // 将库存解锁
            int flag = productMapper.unlockStock(orderMessage.getProductId());
            return flag <= 0;
        }
        // 设置死信不保存
        return false;
    }
}
