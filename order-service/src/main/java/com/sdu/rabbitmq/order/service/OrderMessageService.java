package com.sdu.rabbitmq.order.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdu.rabbitmq.rdts.listener.AbstractMessageListener;
import com.sdu.rabbitmq.rdts.transmitter.TransMessageTransmitter;
import com.sdu.rabbitmq.order.enums.OrderStatus;
import com.sdu.rabbitmq.order.entity.dto.OrderMessageDTO;
import com.sdu.rabbitmq.order.entity.po.OrderDetail;
import com.sdu.rabbitmq.order.repository.OrderDetailMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.io.IOException;

/**
 * 和rabbitmq消息处理相关的通信服务类
 */
@Service("OrderMessageService")
@Slf4j
public class OrderMessageService extends AbstractMessageListener {

    @Value("${rabbitmq.exchange.order-delivery}")
    private String orderDeliveryExchange;

    @Value("${rabbitmq.exchange.order-settlement}")
    private String orderSettlementSendExchange;

    @Value("${rabbitmq.exchange.order-reward}")
    private String orderRewardExchange;

    @Value("${rabbitmq.delivery-routing-key}")
    public String deliveryRoutingKey;

    @Value("${rabbitmq.settlement-routing-key}")
    public String settlementRoutingKey;

    @Value("${rabbitmq.reward-routing-key}")
    public String rewardRoutingKey;

    @Resource
    private OrderDetailMapper orderDetailMapper;

    @Resource
    private TransMessageTransmitter transmitter;

    ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从mq接收到消息的回调
     * consumerTag 消费者类型
     * message Delivery类型的消息
     */
    @Override
    public void receiveMessage(Message message) {
        log.info("receive message: {}", message);
        try {
            OrderMessageDTO orderMessage = objectMapper.readValue(message.getBody(), OrderMessageDTO.class);
            log.info("Current order status: {}", orderMessage.getOrderStatus());
            // 通过订单状态判断是哪个微服务发来的消息
            switch (orderMessage.getOrderStatus()) {
                // 订单刚创建商家还未确认 是商家发来的消息
                case ORDER_CREATING:
                    // 商家已确认订单并将价格写入
                    if (orderMessage.getConfirmed() && null != orderMessage.getPrice()) {
                        UpdateWrapper<OrderDetail> updateWrapper = new UpdateWrapper<>();
                        updateWrapper.eq("id", orderMessage.getOrderId()).set("status", OrderStatus.RESTAURANT_CONFIRMED)
                                .set("price", orderMessage.getPrice());
                        orderDetailMapper.update(null, updateWrapper);
                        // 设置订单状态
                        orderMessage.setOrderStatus(OrderStatus.RESTAURANT_CONFIRMED);
                        // 给骑手微服务发送消息
                        transmitter.send(orderDeliveryExchange, deliveryRoutingKey, orderMessage);
                    } else {
                        updateOrderFailed(orderMessage.getOrderId());
                    }
                    break;
                // 骑手已确认后 消息的状态还没来得及改为DELIVERYMAN_CONFIRMED，所以还是RESTAURANT_CONFIRMED
                case RESTAURANT_CONFIRMED:
                    // 判断订单已经有了确定的骑手
                    if (null != orderMessage.getDeliverymanId()) {
                        // 更新数据库的订单状态和骑手信息
                        UpdateWrapper<OrderDetail> updateWrapper = new UpdateWrapper<>();
                        updateWrapper.eq("id", orderMessage.getOrderId()).set("status", OrderStatus.DELIVERYMAN_CONFIRMED)
                                .set("deliveryman_id", orderMessage.getDeliverymanId());
                        orderDetailMapper.update(null, updateWrapper);
                        // 设置订单状态
                        orderMessage.setOrderStatus(OrderStatus.DELIVERYMAN_CONFIRMED);
                        // 向结算微服务发送一条消息 发送的方式是扇形广播
                        transmitter.send(orderSettlementSendExchange, settlementRoutingKey, orderMessage);
                    } else {
                        // 如果没有骑手，则直接更新订单的状态为失败
                        updateOrderFailed(orderMessage.getOrderId());
                    }
                    break;
                case DELIVERYMAN_CONFIRMED:
                    // 判断订单是否已经有了结算订单的id
                    if (null != orderMessage.getSettlementId()) {
                        // 更新数据库的订单状态和结算信息
                        UpdateWrapper<OrderDetail> updateWrapper = new UpdateWrapper<>();
                        updateWrapper.eq("id", orderMessage.getOrderId()).set("status", OrderStatus.SETTLEMENT_CONFIRMED)
                                .set("settlement_id", orderMessage.getSettlementId());
                        orderDetailMapper.update(null, updateWrapper);
                        orderMessage.setOrderStatus(OrderStatus.SETTLEMENT_CONFIRMED);
                        // 向积分微服务发送一条消息 发送的方式是topic
                        transmitter.send(orderRewardExchange, rewardRoutingKey, orderMessage);
                    } else {
                        // 如果没有结算id，则直接更新订单的状态为失败
                        updateOrderFailed(orderMessage.getOrderId());
                    }
                    break;
                case SETTLEMENT_CONFIRMED:
                    // 判断订单是否已经有了积分的id
                    if (null != orderMessage.getRewardId()) {
                        // 更新数据库的订单状态和积分信息
                        UpdateWrapper<OrderDetail> updateWrapper = new UpdateWrapper<>();
                        updateWrapper.eq("id", orderMessage.getOrderId()).set("status", OrderStatus.ORDER_CREATED)
                                .set("reward_id", orderMessage.getRewardId());
                        orderDetailMapper.update(null, updateWrapper);
                    } else {
                        // 如果没有积分id，则直接更新订单的状态为失败
                        updateOrderFailed(orderMessage.getOrderId());
                    }
                    break;
                default:
                    break;
            }
        } catch (RuntimeException | IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void updateOrderFailed(Long orderId) {
        UpdateWrapper<OrderDetail> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", orderId).set("status", OrderStatus.FAILED);
        orderDetailMapper.update(null, updateWrapper);
    }
}
