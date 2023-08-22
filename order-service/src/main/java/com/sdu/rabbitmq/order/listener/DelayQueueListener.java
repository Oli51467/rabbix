package com.sdu.rabbitmq.order.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.sdu.rabbitmq.common.commons.enums.OrderStatus;
import com.sdu.rabbitmq.common.domain.dto.OrderMessageDTO;
import com.sdu.rabbitmq.common.domain.po.OrderDetail;
import com.sdu.rabbitmq.common.domain.po.ProductOrderDetail;
import com.sdu.rabbitmq.common.service.order.IOrderService;
import com.sdu.rabbitmq.common.service.product.IProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DelayQueueListener implements ChannelAwareMessageListener {

    @Autowired
    private IOrderService iOrderService;

    @Autowired
    private IProductService iProductService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        OrderMessageDTO orderMessage = objectMapper.readValue(message.getBody(), OrderMessageDTO.class);
        Long orderId = orderMessage.getOrderId();
        OrderDetail orderDetails = iOrderService.queryById(orderId);
        if (orderDetails.getStatus().equals(OrderStatus.WAITING_PAY)) {
            log.error("订单已失效，订单号：{}", orderId);
            // 将该订单关闭
            iOrderService.updateOrderDetailStatus(orderId, OrderStatus.CANCELED);
            // 查询该订单的中所有商品及商品的数量
            for (ProductOrderDetail productOrderDetail : orderMessage.getDetails()) {
                iProductService.unlockStock(productOrderDetail.getProductId(), productOrderDetail.getCount());
            }
        }
        MessageProperties properties = message.getMessageProperties();
        channel.basicAck(properties.getDeliveryTag(), false);
    }
}
