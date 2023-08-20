package com.sdu.rabbitmq.common.domain.po;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ProductOrderDetail {

    private Long productId;

    private Integer count;
}
