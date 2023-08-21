package com.sdu.rabbitmq.order.listener;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.sdu.rabbitmq.common.commons.enums.OrderStatus;
import com.sdu.rabbitmq.common.domain.dto.OrderMessageDTO;
import com.sdu.rabbitmq.common.domain.po.OrderDetail;
import com.sdu.rabbitmq.common.domain.po.ProductOrderDetail;
import com.sdu.rabbitmq.common.feign.StockFeign;
import com.sdu.rabbitmq.order.repository.OrderDetailMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class DelayQueueListener implements ChannelAwareMessageListener {

    @Resource
    private OrderDetailMapper orderDetailMapper;

    @Resource
    private StockFeign stockFeign;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        OrderMessageDTO orderMessage = objectMapper.readValue(message.getBody(), OrderMessageDTO.class);
        QueryWrapper<OrderDetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", orderMessage.getOrderId());
        OrderDetail orderDetails = orderDetailMapper.selectOne(queryWrapper);
        if (orderDetails.getStatus().equals(OrderStatus.WAITING_PAY)) {
            log.error("订单已失效，订单号：{}", orderMessage.getOrderId());
            // 将该订单关闭
            UpdateWrapper<OrderDetail> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", orderMessage.getOrderId()).set("status", OrderStatus.CANCELED.toString());
            orderDetailMapper.update(null, updateWrapper);
            // 查询该订单的中所有商品及商品的数量
            for (ProductOrderDetail productOrderDetail : orderMessage.getDetails()) {
                stockFeign.unlockStock(productOrderDetail.getProductId(), productOrderDetail.getCount());
            }
        }
        MessageProperties properties = message.getMessageProperties();
        channel.basicAck(properties.getDeliveryTag(), false);
    }
}
