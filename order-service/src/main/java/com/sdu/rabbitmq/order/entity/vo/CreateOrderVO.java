package com.sdu.rabbitmq.order.entity.vo;

import com.sdu.rabbitmq.common.domain.po.ProductOrderDetail;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class CreateOrderVO {

    // 用户id
    private Long accountId;

    // 地址
    private String address;

    // 订单细节
    private List<ProductOrderDetail> details;
}
