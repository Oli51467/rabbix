package com.sdu.rabbitmq.order.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sdu.rabbitmq.common.annotation.Idempotent;
import com.sdu.rabbitmq.common.annotation.RedissonLock;
import com.sdu.rabbitmq.common.commons.enums.OrderStatus;
import com.sdu.rabbitmq.common.commons.enums.ProductStatus;
import com.sdu.rabbitmq.common.domain.dto.OrderMessageDTO;
import com.sdu.rabbitmq.common.domain.po.OrderDetail;
import com.sdu.rabbitmq.common.domain.po.Product;
import com.sdu.rabbitmq.common.domain.po.ProductOrderDetail;
import com.sdu.rabbitmq.common.response.ResponseResult;
import com.sdu.rabbitmq.common.response.exception.BusinessException;
import com.sdu.rabbitmq.common.response.exception.ExceptionEnum;
import com.sdu.rabbitmq.common.service.order.IOrderService;
import com.sdu.rabbitmq.common.service.product.IProductService;
import com.sdu.rabbitmq.common.utils.RedisUtil;
import com.sdu.rabbitmq.order.entity.vo.CreateOrderVO;
import com.sdu.rabbitmq.order.service.OrderService;
import com.sdu.rabbitmq.rdts.transmitter.TransMessageTransmitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.sdu.rabbitmq.common.commons.RedisKey.*;

@Service("OrderService")
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Resource
    private IOrderService iOrderService;

    @Resource
    private IProductService iProductService;

    @Value("${rabbitmq.exchange.order-restaurant}")
    private String exchangeOrderRestaurant;

    @Value("${rabbitmq.restaurant-routing-key}")
    public String restaurantRoutingKey;

    @Value("${rabbitmq.release-routing-key}")
    public String releaseRoutingKey;

    @Resource
    private TransMessageTransmitter transmitter;

    public ResponseResult createOrder(CreateOrderVO createOrderVO) {
        log.info("createOrder:orderCreateVO: {}", createOrderVO);
        List<ProductOrderDetail> productOrderDetails = createOrderVO.getDetails();
        boolean check = checkStockAndLock(Thread.currentThread().getId(), productOrderDetails);
        if (!check) {
            throw new BusinessException(ExceptionEnum.NO_STOCK);
        }
        // 商品存在且库存足够 创建订单 设置订单状态为创建中
        OrderDetail order = iOrderService.createOrder(createOrderVO.getAddress(), createOrderVO.getAccountId());

        // 创建消息队列传输对象
        OrderMessageDTO orderMessage = new OrderMessageDTO();
        orderMessage.setOrderId(order.getId());
        orderMessage.setDetails(productOrderDetails);
        orderMessage.setAccountId(order.getAccountId());
        orderMessage.setOrderStatus(OrderStatus.WAITING_PAY);
        // 下单还未支付时，将具体的下单商品的id和数量存储在redis中
        Map<String, Object> orderProductDetails = new HashMap<>();
        for (ProductOrderDetail productOrderDetail : productOrderDetails) {
            orderProductDetails.put(productOrderDetail.getProductId().toString(), productOrderDetail.getCount().toString());
        }
        RedisUtil.hMultiSet(getKeyWithString(PRODUCT_DETAILS_KEY, order.getId().toString()), orderProductDetails, 80);
        // 将订单信息发送到延迟队列 等待支付
        try {
            transmitter.send(exchangeOrderRestaurant, releaseRoutingKey, orderMessage);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        log.info("send to delay queue!");
        return ResponseResult.ok(order.getId());
    }

    @RedissonLock(prefixKey = "rabbit:stock", key = "#id")
    private boolean checkStockAndLock(long id, List<ProductOrderDetail> productOrderDetails) {
        // 判断是否有库存
        for (ProductOrderDetail productOrderDetail : productOrderDetails) {
            // 查询商品
            Product product = iProductService.queryById(productOrderDetail.getProductId());
            if (null == product) {
                throw new BusinessException(ExceptionEnum.NO_PRODUCT);
            }
            if (product.getStock() - product.getStockLocked() < productOrderDetail.getCount() || product.getStatus().equals(ProductStatus.NOT_AVAILABLE)) {
                return false;
            }
        }
        for (ProductOrderDetail productOrderDetail : productOrderDetails) {
            // 锁定库存
            iProductService.lockStock(productOrderDetail.getProductId(), productOrderDetail.getCount());
        }
        return true;
    }

    /**
     * 支付订单
     * @param orderId 支付订单id
     * @return ResponseResult
     */
    @Override
    @Idempotent(prefix = "rabbit:pay", key = "#orderId", waitTime = 2, unit = TimeUnit.MINUTES)
    public ResponseResult payOrder(Long orderId) {
        // 从数据库中将订单信息查出
        OrderDetail orderDetail = iOrderService.queryById(orderId);
        // 如果订单不存在或者订单已经不是待支付状态，则支付失败
        if (null == orderDetail) {
            return ResponseResult.fail("订单不存在");
        }
        // 如果redis中已经没有下单的商品详细信息，则订单已失效
        if (!orderDetail.getStatus().equals(OrderStatus.WAITING_PAY) || !RedisUtil.hasKey(getKeyWithString(PRODUCT_DETAILS_KEY, orderId.toString()))) {
            return ResponseResult.fail("订单已失效");
        }
        // 先将商品详细信息取出，防止业务操作时间过长导致失效
        Map<Object, Object> orderProductDetails = RedisUtil.hMultiGet(getKeyWithString(PRODUCT_DETAILS_KEY, orderId.toString()));
        log.info("orderProductDetails: {}", orderProductDetails);
        // 订单存在，将订单状态修改状态为ORDER_CREATING
        iOrderService.updateOrderDetailStatus(orderId, OrderStatus.ORDER_CREATING);
        // 创建消息队列传输对象 状态为ORDER_CREATING
        OrderMessageDTO orderMessage = new OrderMessageDTO();
        orderMessage.setOrderId(orderDetail.getId());
        orderMessage.setAccountId(orderDetail.getAccountId());
        orderMessage.setOrderStatus(OrderStatus.ORDER_CREATING);
        List<ProductOrderDetail> productOrderDetails = new ArrayList<>();
        // 遍历从redis中得到的商品详细信息，将其设置在消息传输对象中
        for (Map.Entry<Object, Object> entry : orderProductDetails.entrySet()) {
            ProductOrderDetail productOrderDetail = new ProductOrderDetail();
            productOrderDetail.setProductId(Long.parseLong(entry.getKey().toString()));
            productOrderDetail.setCount(Integer.parseInt(entry.getValue().toString()));
            productOrderDetails.add(productOrderDetail);
        }
        orderMessage.setDetails(productOrderDetails);
        // 将订单信息发送到餐厅微服务
        try {
            transmitter.send(exchangeOrderRestaurant, restaurantRoutingKey, orderMessage);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        log.info("send to restaurant queue!");
        return ResponseResult.ok();
    }
}
