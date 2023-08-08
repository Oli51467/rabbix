package com.sdu.rabbitmq.order.entity.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CreateOrderVO {

    private Long accountId;

    private String address;

    private Long productId;
}
