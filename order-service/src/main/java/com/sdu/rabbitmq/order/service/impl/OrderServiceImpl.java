package com.sdu.rabbitmq.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import com.sdu.rabbitmq.common.utils.RedisUtil;
import com.sdu.rabbitmq.common.utils.SnowUtil;
import com.sdu.rabbitmq.order.entity.dto.PayOrderDTO;
import com.sdu.rabbitmq.order.repository.OrderDetailMapper;
import com.sdu.rabbitmq.order.repository.ProductMapper;
import com.sdu.rabbitmq.order.service.OrderService;
import com.sdu.rabbitmq.rdts.transmitter.TransMessageTransmitter;
import com.sdu.rabbitmq.order.entity.vo.CreateOrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

import static com.sdu.rabbitmq.common.commons.RedisKey.PRODUCT_DETAILS_KEY;
import static com.sdu.rabbitmq.common.commons.RedisKey.getKey;

@Service("OrderService")
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Resource
    private OrderDetailMapper orderDetailMapper;

    @Resource
    private ProductMapper productMapper;

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
        OrderDetail order = new OrderDetail();
        order.setId(SnowUtil.getSnowflakeNextId());
        order.setAddress(createOrderVO.getAddress());
        order.setAccountId(createOrderVO.getAccountId());
        order.setCreateTime(new Date());
        order.setStatus(OrderStatus.WAITING_PAY);
        orderDetailMapper.insert(order);

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
        RedisUtil.hMultiSet(getKey(PRODUCT_DETAILS_KEY, order.getId()), orderProductDetails, 80);
        // 将订单信息发送到延迟队列 等待支付
        try {
            transmitter.send(exchangeOrderRestaurant, releaseRoutingKey, orderMessage);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        log.info("send to delay queue!");
        return ResponseResult.ok(order.getId());
    }

    @RedissonLock(prefixKey = "rabbit.stock", key = "#id")
    private boolean checkStockAndLock(long id, List<ProductOrderDetail> productOrderDetails) {
        // 判断是否有库存
        for (ProductOrderDetail productOrderDetail : productOrderDetails) {
            QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("id", productOrderDetail.getProductId());
            Product product = productMapper.selectOne(queryWrapper);
            if (null == product) {
                throw new BusinessException(ExceptionEnum.NO_PRODUCT);
            }
            if (product.getStock() - product.getStockLocked() < productOrderDetail.getCount() || product.getStatus().equals(ProductStatus.NOT_AVAILABLE)) {
                return false;
            }
        }
        for (ProductOrderDetail productOrderDetail : productOrderDetails) {
            // 锁定库存
            productMapper.lockStock(productOrderDetail.getProductId(), productOrderDetail.getCount());
        }
        return true;
    }

    /**
     * 支付订单
     * @param payOrderDTO 支付订单dto
     * @return ResponseResult
     */
    @Override
    public ResponseResult payOrder(PayOrderDTO payOrderDTO) {
        // 拿到订单id
        String orderId = payOrderDTO.getOrderId();
        // 从数据库中将订单信息查出
        QueryWrapper<OrderDetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", Long.parseLong(orderId));
        OrderDetail orderDetail = orderDetailMapper.selectOne(queryWrapper);
        // 如果订单不存在或者订单已经不是待支付状态，则支付失败
        if (null == orderDetail || !orderDetail.getStatus().equals(OrderStatus.WAITING_PAY)) {
            return ResponseResult.fail("订单已失效");
        }
        // 如果redis中已经没有下单的商品详细信息，则订单已失效
        if (!RedisUtil.hasKey(getKey(PRODUCT_DETAILS_KEY, Long.parseLong(orderId)))) {
            return ResponseResult.fail("订单已失效");
        }
        // 先将商品详细信息取出，防止业务操作时间过长导致失效
        Map<Object, Object> orderProductDetails = RedisUtil.hMultiGet(getKey(PRODUCT_DETAILS_KEY, Long.parseLong(orderId)));
        log.info("orderProductDetails: {}", orderProductDetails);
        // 订单存在，将订单状态修改状态为ORDER_CREATING
        UpdateWrapper<OrderDetail> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", orderId).set("status", OrderStatus.ORDER_CREATING.toString());
        orderDetailMapper.update(null, updateWrapper);
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
