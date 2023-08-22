package com.sdu.rabbitmq.restaurant.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdu.rabbitmq.common.commons.enums.OrderStatus;
import com.sdu.rabbitmq.common.domain.dto.OrderMessageDTO;
import com.sdu.rabbitmq.common.domain.po.ProductOrderDetail;
import com.sdu.rabbitmq.common.service.order.IOrderService;
import com.sdu.rabbitmq.common.service.product.IProductService;
import com.sdu.rabbitmq.rdts.listener.AbstractDlxListener;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

@Component
public class DlxListener extends AbstractDlxListener {

    @Resource
    private IOrderService iOrderService;

    @Resource
    private IProductService iProductService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean receiveMessage(Message message) throws IOException {
        OrderMessageDTO orderMessage = objectMapper.readValue(message.getBody(), OrderMessageDTO.class);
        // 将该订单关闭
        iOrderService.updateOrderDetailStatus(orderMessage.getOrderId(), OrderStatus.FAILED);
        // 查询该订单的中所有商品及商品的数量
        for (ProductOrderDetail productOrderDetail : orderMessage.getDetails()) {
            iProductService.unlockStock(productOrderDetail.getProductId(), productOrderDetail.getCount());
        }
        return true;
    }
}

