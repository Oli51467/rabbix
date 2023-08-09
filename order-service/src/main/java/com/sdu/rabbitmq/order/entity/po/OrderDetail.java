package com.sdu.rabbitmq.order.entity.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sdu.rabbitmq.order.enums.OrderStatus;
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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private Date createTime;
}
