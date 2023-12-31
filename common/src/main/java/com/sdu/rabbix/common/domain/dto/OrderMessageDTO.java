package com.sdu.rabbix.common.domain.dto;

import com.sdu.rabbix.common.commons.enums.OrderStatus;
import com.sdu.rabbix.common.domain.po.ProductOrderDetail;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

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
    private List<ProductOrderDetail> details;

    // 用户id
    private Long accountId;

    // 积分奖励数量
    private BigDecimal rewardAmount;

    // 是否已确认
    private Boolean confirmed;
}
