package com.sdu.rabbix.order.service;

import com.sdu.rabbix.common.response.ResponseResult;
import com.sdu.rabbix.order.entity.dto.AliPayDTO;

import java.util.Map;

public interface PayService {

    ResponseResult doPay(AliPayDTO aliPayDTO);

    ResponseResult payOrder(Long orderId);

    ResponseResult checkRsaV1(Map<String, String> params);

    ResponseResult refund(Long orderId);
}
