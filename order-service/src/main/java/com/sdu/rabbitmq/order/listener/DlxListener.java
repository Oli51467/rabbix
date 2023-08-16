package com.sdu.rabbitmq.order.listener;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdu.rabbitmq.order.entity.dto.OrderMessageDTO;
import com.sdu.rabbitmq.order.entity.po.OrderDetail;
import com.sdu.rabbitmq.order.enums.OrderStatus;
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

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean receiveMessage(Message message) throws IOException {
        OrderMessageDTO orderMessage = objectMapper.readValue(message.getBody(), OrderMessageDTO.class);
        QueryWrapper<OrderDetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", orderMessage.getOrderId());
        OrderDetail orderDetail = orderDetailMapper.selectOne(queryWrapper);
        if (orderDetail.getStatus().equals(OrderStatus.WAITING_PAY)) {
            // 将该订单关闭
            UpdateWrapper<OrderDetail> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", orderMessage.getOrderId()).set("status", OrderStatus.FAILED.toString());
            orderDetailMapper.update(null, updateWrapper);
        }
        // 设置死信不保存
        return false;
    }
}
