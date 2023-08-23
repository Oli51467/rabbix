package com.sdu.rabbix.common.domain.po;

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
