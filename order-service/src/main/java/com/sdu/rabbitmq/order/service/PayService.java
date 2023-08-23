package com.sdu.rabbitmq.order.service;

import com.sdu.rabbitmq.common.response.ResponseResult;
import com.sdu.rabbitmq.order.entity.dto.AliPayDTO;

import java.util.Map;

public interface PayService {

    ResponseResult doPay(AliPayDTO aliPayDTO);

    ResponseResult payOrder(Long orderId);

    ResponseResult checkRsaV1(Map<String, String> params);

    ResponseResult refund(Long orderId);
}
