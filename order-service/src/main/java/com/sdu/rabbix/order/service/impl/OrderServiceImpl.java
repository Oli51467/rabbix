package com.sdu.rabbix.order.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sdu.rabbix.common.annotation.FrequencyControl;
import com.sdu.rabbix.common.annotation.RedissonLock;
import com.sdu.rabbix.common.commons.enums.OrderStatus;
import com.sdu.rabbix.common.commons.enums.ProductStatus;
import com.sdu.rabbix.common.domain.dto.OrderMessageDTO;
import com.sdu.rabbix.common.domain.po.OrderDetail;
import com.sdu.rabbix.common.domain.po.Product;
import com.sdu.rabbix.common.domain.po.ProductOrderDetail;
import com.sdu.rabbix.common.response.ResponseResult;
import com.sdu.rabbix.common.response.exception.BusinessException;
import com.sdu.rabbix.common.response.exception.ExceptionEnum;
import com.sdu.rabbix.common.service.order.IOrderService;
import com.sdu.rabbix.common.service.product.IProductService;
import com.sdu.rabbix.common.utils.RedisUtil;
import com.sdu.rabbix.order.entity.vo.CreateOrderVO;
import com.sdu.rabbix.order.service.OrderService;
import com.sdu.rabbix.transaction.transmitter.TransMessageTransmitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.sdu.rabbix.common.commons.RedisKey.PRODUCT_DETAILS_KEY;
import static com.sdu.rabbix.common.commons.RedisKey.getKeyWithString;

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

    @FrequencyControl(time = 60, count = 10, target = FrequencyControl.Target.IP)
    public ResponseResult createOrder(CreateOrderVO createOrderVO) {
        log.info("createOrder:orderCreateVO: {}", createOrderVO);
        List<ProductOrderDetail> productOrderDetails = createOrderVO.getDetails();
        // 计算待支付金额并判断商品是否可以下单
        BigDecimal totalPrice = new BigDecimal(0);
        for (ProductOrderDetail orderDetail : productOrderDetails) {
            Product product = iProductService.queryById(orderDetail.getProductId());
            log.info("订单产品信息: {}", product);
            // 确定商品是否可下单
            if (product.getStatus() == ProductStatus.AVAILABLE) {
                BigDecimal cnt = new BigDecimal(orderDetail.getCount());
                totalPrice = totalPrice.add(product.getPrice().multiply(cnt));
            } else {
                return ResponseResult.ok("部分商品不可下单");
            }
        }
        // 加锁判断库存是否足够
        checkStockAndLock(productOrderDetails);
        // 商品存在且库存足够 创建订单 设置订单状态为创建中
        OrderDetail order = iOrderService.createOrder(createOrderVO.getAddress(), createOrderVO.getAccountId(), totalPrice);
        // 创建消息队列传输对象
        OrderMessageDTO orderMessage = new OrderMessageDTO();
        orderMessage.setOrderId(order.getId());
        orderMessage.setDetails(productOrderDetails);
        orderMessage.setAccountId(order.getAccountId());
        orderMessage.setOrderStatus(OrderStatus.WAITING_PAY);
        orderMessage.setPrice(totalPrice);
        // 下单还未支付时，将具体的下单商品的id和数量存储在redis中
        Map<String, Object> orderProductDetails = productOrderDetails.stream().collect(Collectors.toMap(
                productOrderDetail -> productOrderDetail.getProductId().toString(),
                productOrderDetail -> productOrderDetail.getCount().toString(),
                (existingVal, newVal) -> existingVal
        ));
        RedisUtil.hMultiSet(getKeyWithString(PRODUCT_DETAILS_KEY, order.getId().toString()), orderProductDetails, 80);
        // 将订单信息发送到延迟队列 等待支付
        try {
            transmitter.send(exchangeOrderRestaurant, releaseRoutingKey, orderMessage);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        log.info("下单成功!等待支付");
        return ResponseResult.ok(order.getId());
    }

    @RedissonLock(prefixKey = "rabbit:stock", key = "#productOrderDetails.getProductId()")
    private void checkStockAndLock(List<ProductOrderDetail> productOrderDetails) {
        // 判断是否有库存
        for (ProductOrderDetail productOrderDetail : productOrderDetails) {
            // 查询商品
            Product product = iProductService.queryById(productOrderDetail.getProductId());
            if (null == product) {
                throw new BusinessException(ExceptionEnum.NO_PRODUCT);
            }
            if (product.getStock() - product.getStockLocked() < productOrderDetail.getCount() || product.getStatus().equals(ProductStatus.NOT_AVAILABLE)) {
                throw new BusinessException(ExceptionEnum.NO_STOCK);
            }
        }
        for (ProductOrderDetail productOrderDetail : productOrderDetails) {
            // 锁定库存
            iProductService.lockStock(productOrderDetail.getProductId(), productOrderDetail.getCount());
        }
    }
}
