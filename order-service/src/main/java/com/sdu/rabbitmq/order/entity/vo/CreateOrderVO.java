package com.sdu.rabbitmq.order.entity.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CreateOrderVO {

    // 用户id
    private Long accountId;

    // 地址
    private String address;

    // 产品id
    private Long productId;
}
