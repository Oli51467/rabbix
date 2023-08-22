package com.sdu.rabbitmq.order.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdu.rabbitmq.common.commons.enums.OrderStatus;
import com.sdu.rabbitmq.common.domain.dto.OrderMessageDTO;
import com.sdu.rabbitmq.common.domain.po.OrderProduct;
import com.sdu.rabbitmq.common.domain.po.ProductOrderDetail;
import com.sdu.rabbitmq.common.service.order.IOrderService;
import com.sdu.rabbitmq.common.service.product.IProductService;
import com.sdu.rabbitmq.order.repository.OrderProductMapper;
import com.sdu.rabbitmq.rdts.listener.AbstractMessageListener;
import com.sdu.rabbitmq.rdts.transmitter.TransMessageTransmitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

/**
 * 和rabbitmq消息处理相关的通信服务类
 */
@Service("OrderMessageService")
@Slf4j
public class OrderMessageService extends AbstractMessageListener {

    @Value("${rabbitmq.exchange.order-delivery}")
    private String orderDeliveryExchange;

    @Value("${rabbitmq.exchange.order-reward}")
    private String orderRewardExchange;

    @Value("${rabbitmq.delivery-routing-key}")
    public String deliveryRoutingKey;

    @Value("${rabbitmq.reward-routing-key}")
    public String rewardRoutingKey;

    @Resource
    private IOrderService iOrderService;

    @Resource
    private IProductService iProductService;

    @Resource
    private OrderProductMapper orderProductMapper;

    @Resource
    private TransMessageTransmitter transmitter;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 从mq接收到消息的回调
     * consumerTag 消费者类型
     * message Delivery类型的消息
     */
    @Override
    public void receiveMessage(Message message) throws IOException {
        String messageBody = new String(message.getBody());
        log.info("接收到的消息体: {}", messageBody);
        OrderMessageDTO orderMessage = objectMapper.readValue(message.getBody(), OrderMessageDTO.class);
        Long orderId = orderMessage.getOrderId();
        log.info("当前订单状态: {}", orderMessage.getOrderStatus());
        // 通过订单状态判断是哪个微服务发来的消息
        switch (orderMessage.getOrderStatus()) {
            // 订单刚创建商家还未确认 是商家发来的消息
            case ORDER_CREATING:
                // 商家已确认订单并将价格写入
                if (orderMessage.getConfirmed() && null != orderMessage.getPrice()) {
                    iOrderService.updateOrderDetailStatusAndPrice(orderId, OrderStatus.RESTAURANT_CONFIRMED, orderMessage.getPrice());
                    // 设置订单状态
                    orderMessage.setOrderStatus(OrderStatus.RESTAURANT_CONFIRMED);
                    // 给骑手微服务发送消息
                    transmitter.send(orderDeliveryExchange, deliveryRoutingKey, orderMessage);
                } else {
                    updateOrderFailed(orderId);
                }
                break;
            // 骑手已确认后 消息的状态还没来得及改为DELIVERYMAN_CONFIRMED，所以还是RESTAURANT_CONFIRMED
            case RESTAURANT_CONFIRMED:
                // 判断订单已经有了确定的骑手
                if (null != orderMessage.getDeliverymanId()) {
                    // 更新数据库的订单状态和骑手信息
                    iOrderService.updateOrderDetailStatusAndDelivery(orderId, OrderStatus.DELIVERYMAN_CONFIRMED, orderMessage.getDeliverymanId());
                    // 设置订单状态
                    orderMessage.setOrderStatus(OrderStatus.DELIVERYMAN_CONFIRMED);
                    // 向积分微服务发送一条消息 发送的方式是topic
                    transmitter.send(orderRewardExchange, rewardRoutingKey, orderMessage);
                } else {
                    // 如果没有骑手，则直接更新订单的状态为失败
                    updateOrderFailed(orderId);
                }
                break;
            case DELIVERYMAN_CONFIRMED:
                // 判断订单是否已经有了积分的id
                if (null != orderMessage.getRewardId()) {
                    // 更新数据库的订单状态和积分信息
                    iOrderService.updateOrderDetailStatusAndReward(orderId, OrderStatus.ORDER_CREATED, orderMessage.getRewardId());
                    List<ProductOrderDetail> productOrderDetails = orderMessage.getDetails();
                    for (ProductOrderDetail productOrderDetail : productOrderDetails) {
                        iProductService.deductStock(productOrderDetail.getProductId(), productOrderDetail.getCount());
                        OrderProduct orderProduct = new OrderProduct();
                        orderProduct.setOrderId(orderId);
                        orderProduct.setProductId(productOrderDetail.getProductId());
                        orderProduct.setCount(productOrderDetail.getCount());
                        orderProductMapper.insert(orderProduct);
                    }
                } else {
                    // 如果没有积分id，则直接更新订单的状态为失败
                    updateOrderFailed(orderId);
                }
                break;
            default:
                break;
        }
    }

    private void updateOrderFailed(Long orderId) {
        iOrderService.updateOrderDetailStatus(orderId, OrderStatus.FAILED);
    }
}
