package com.sdu.rabbitmq.common.domain.po;

import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class OrderProduct {

    private Long orderId;

    private Long productId;

    private Integer count;
}
