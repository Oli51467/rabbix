package com.sdu.rabbix.order.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AliPayDTO {

    /**
     * 订单编号
     */
    private String outTradeNo;
    /**
     * 付款金额，必填
     */
    private String totalAmount;
    /**
     * 订单名称，必填
     */
    private String subject;
    /**
     * 商品描述，可空
     */
    private String body;
    /**
     * 同步回调地址
     */
    private String returnUrl;
    /**
     * 异步回调地址
     */
    private String notifyUrl;
}
