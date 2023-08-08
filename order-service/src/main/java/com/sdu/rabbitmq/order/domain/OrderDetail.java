package com.sdu.rabbitmq.order.domain;

import com.sdu.rabbitmq.order.common.enums.OrderStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@ToString
public class OrderDetail {

    private Long id;

    private OrderStatus status;

    private String address;

    private Long accountId;

    private Long productId;

    private Long deliverymanId;

    private Long settlementId;

    private Long rewardId;

    private BigDecimal price;

    private Date createTime;
}
