package com.sdu.rabbix.order.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sdu.rabbix.common.commons.enums.OrderStatus;
import com.sdu.rabbix.common.domain.dto.OrderMessageDTO;
import com.sdu.rabbix.common.domain.po.OrderDetail;
import com.sdu.rabbix.common.domain.po.OrderProduct;
import com.sdu.rabbix.common.domain.po.ProductOrderDetail;
import com.sdu.rabbix.common.response.ResponseCode;
import com.sdu.rabbix.common.response.ResponseResult;
import com.sdu.rabbix.common.service.order.IOrderService;
import com.sdu.rabbix.common.service.product.IProductService;
import com.sdu.rabbix.common.utils.RedisUtil;
import com.sdu.rabbix.order.config.AlipayProperties;
import com.sdu.rabbix.order.entity.dto.AliPayDTO;
import com.sdu.rabbix.order.entity.dto.RefundDTO;
import com.sdu.rabbix.order.repository.OrderProductMapper;
import com.sdu.rabbix.order.service.PayService;
import com.sdu.rabbix.transaction.transmitter.TransMessageTransmitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.sdu.rabbix.common.commons.RedisKey.PRODUCT_DETAILS_KEY;
import static com.sdu.rabbix.common.commons.RedisKey.getKeyWithString;

@Service("PayService")
@Slf4j
public class PayServiceImpl implements PayService {

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private AlipayProperties alipayProperties;

    @Autowired
    private IOrderService iOrderService;

    @Autowired
    private IProductService iProductService;

    @Autowired
    private OrderProductMapper orderProductMapper;

    @Value("${rabbitmq.exchange.order-restaurant}")
    private String exchangeOrderRestaurant;

    @Value("${rabbitmq.restaurant-routing-key}")
    public String restaurantRoutingKey;

    @Value("${alipay.returnUrl}")
    private String returnUrl;

    @Value("${alipay.notifyUrl}")
    private String notifyUrl;

    @Resource
    private TransMessageTransmitter transmitter;

    /**
     * 支付订单
     * 支付宝那边有幂等性操作，不会支付两次。不需要Idempotent注解
     *  当下单那一刻，如果订单已经支付过，不会回调。
     * @param orderId 订单号
     * @return ResponseResult
     */
    @Override
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
        if (orderProductDetails.isEmpty()) {
            return ResponseResult.fail("订单已失效");
        }
        log.info("订单详情: {}", orderProductDetails);
        // **创建阿里支付实体并填入支付信息**
        AliPayDTO aliPayDTO = new AliPayDTO();
        aliPayDTO.setOutTradeNo(orderId.toString());
        aliPayDTO.setTotalAmount(orderDetail.getPrice().toString());
        aliPayDTO.setSubject(orderDetail.getAddress());
        aliPayDTO.setBody(orderDetail.getAddress());
        aliPayDTO.setReturnUrl(returnUrl);
        aliPayDTO.setNotifyUrl(notifyUrl);
        // 调用阿里云支付服务
        ResponseResult payStatus = doPay(aliPayDTO);
        if (payStatus.getCode() == ResponseCode.FAIL.getCode()) {
            iOrderService.updateOrderStatus(orderId, OrderStatus.FAILED);
            return ResponseResult.fail("支付失败");
        }
        // 支付成功，将订单状态修改状态为ORDER_CREATING
        iOrderService.updateOrderStatus(orderId, OrderStatus.ORDER_CREATING);
        // 创建消息队列传输对象 状态为ORDER_CREATING
        OrderMessageDTO orderMessage = new OrderMessageDTO();
        orderMessage.setOrderId(orderDetail.getId());
        orderMessage.setAccountId(orderDetail.getAccountId());
        orderMessage.setOrderStatus(OrderStatus.ORDER_CREATING);
        orderMessage.setPrice(orderDetail.getPrice());
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
        log.info("发送成功，等待餐厅确认!");
        return ResponseResult.ok();
    }

    @Override
    public ResponseResult doPay(AliPayDTO aliPayDTO) {
        /*// 设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        // 同步回调地址
        alipayRequest.setReturnUrl(aliPayDTO.getReturnUrl());
        // 异步回调地址
        alipayRequest.setNotifyUrl(aliPayDTO.getNotifyUrl());
        // 订单过期时间
        String expireTime = DateTimeUtils.getCurrentDateTimePlusOneMinute(1L);

        alipayRequest.setBizContent("{\"out_trade_no\":\"" + aliPayDTO.getOutTradeNo() + "\","
                + "\"total_amount\":\"" + aliPayDTO.getTotalAmount() + "\","
                + "\"subject\":\"" + aliPayDTO.getSubject() + "\","
                + "\"body\":\"" + aliPayDTO.getBody() + "\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}"
                + "\"timeout_express\":\"1m\"}"                 // 设置订单过期时间为1分钟
                + "\"time_expire\":\"" + expireTime + "\"}");   // 设置订单过期时间
        String htmlContent;
        try {
            htmlContent = alipayClient.pageExecute(alipayRequest).getBody();
        } catch (AlipayApiException exception) {
            return ResponseResult.fail(exception.getMessage());
        }*/
        return ResponseResult.ok();
    }

    @Override
    public ResponseResult checkRsaV1(Map<String, String> params) {
        boolean signVerified;
        try {
            signVerified = AlipaySignature.rsaCheckV1(params,
                    alipayProperties.getAlipayPublicKey(),
                    alipayProperties.getCharset(),
                    alipayProperties.getSignType()); //调用SDK验证签名
        } catch (AlipayApiException e) {
            return ResponseResult.fail(false);
        }
        return ResponseResult.ok(signVerified);
    }

    @Override
    @Transactional
    public ResponseResult refund(Long orderId) {
        // 从数据库中将订单信息查出
        OrderDetail orderDetail = iOrderService.queryById(orderId);
        // 如果订单不存在，则退款失败
        if (null == orderDetail) {
            return ResponseResult.fail("订单不存在");
        }
        // 如果redis中已经没有下单的商品详细信息，则订单已失效
        if (orderDetail.getStatus().equals(OrderStatus.WAITING_PAY)) {
            return ResponseResult.fail("订单未支付");
        }
        // **创建阿里云退款实体并填入支付信息**
        RefundDTO refundDTO = new RefundDTO();
        refundDTO.setOutTradeNo(orderId.toString());
        refundDTO.setRefundAmount(orderDetail.getPrice().toString());
        refundDTO.setRefundReason("None");
        // 调用阿里云退款接口
        ResponseResult refundStatus = doRefund(refundDTO);
        if (null == refundStatus || refundStatus.getCode() == ResponseCode.FAIL.getCode()) {
            return ResponseResult.fail("退款失败");
        }
        // 退款成功，更新数据库的订单状态
        iOrderService.updateOrderStatus(orderId, OrderStatus.REFUND);
        // 将库存还原
        // 拿出该订单下单的所有商品，依次还原库存
        List<OrderProduct> orderProducts = orderProductMapper.queryByOrderId(orderId);
        for (OrderProduct orderProduct : orderProducts) {
            iProductService.restoreStock(orderProduct.getProductId(), orderProduct.getCount());
        }
        return ResponseResult.ok();
    }

    private ResponseResult doRefund(RefundDTO refundDTO) {
        AlipayTradeRefundRequest alipayRequest = new AlipayTradeRefundRequest();
        alipayRequest.setBizContent("{\"out_trade_no\":\"" + refundDTO.getOutTradeNo() + "\","
                + "\"trade_no\":\"\","
                + "\"refund_amount\":\"" + refundDTO.getRefundAmount() + "\","
                + "\"refund_reason\":\"" + refundDTO.getRefundReason() + "\","
                + "\"out_request_no\":\"\"}");
        // 请求
        AlipayTradeRefundResponse res;
        try {
            res = alipayClient.execute(alipayRequest);
        } catch (AlipayApiException e) {
            return ResponseResult.fail("退款失败");
        }
        return ResponseResult.ok(res.isSuccess());
    }
}
