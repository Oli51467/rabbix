package com.sdu.rabbitmq.reward.entity.dto;

import com.sdu.rabbitmq.reward.enums.OrderStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class OrderMessageDTO {

    // 订单号
    private Long orderId;

    // 订单状态
    private OrderStatus orderStatus;

    // 价格
    private BigDecimal price;

    // 骑手id
    private Long deliverymanId;

    // 产品id
    private Long productId;

    // 用户id
    private Long accountId;

    // 结算id
    private Long settlementId;

    // 积分结算id
    private Long rewardId;

    // 积分奖励数量
    private BigDecimal rewardAmount;

    // 是否已确认
    private Boolean confirmed;
}
