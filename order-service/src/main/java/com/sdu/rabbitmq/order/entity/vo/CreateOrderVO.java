package com.sdu.rabbitmq.order.entity.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CreateOrderVO {
    private Integer accountId;
    private String address;
    private Integer productId;
}
