package com.sdu.rabbitmq.restaurant.entity.dto;

import com.sdu.rabbitmq.common.commons.enums.OrderStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class OrderMessageDTO {

    private Long orderId;

    private OrderStatus orderStatus;

    private BigDecimal price;

    private Long deliverymanId;

    private Long productId;

    private Long accountId;

    private Long settlementId;

    private Long rewardId;

    private BigDecimal rewardAmount;

    private Boolean confirmed;
}
