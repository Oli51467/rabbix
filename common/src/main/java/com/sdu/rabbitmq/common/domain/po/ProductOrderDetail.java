package com.sdu.rabbitmq.common.domain.po;

import lombok.*;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ProductOrderDetail {

    @NotNull(message = "商品id不能为空")
    private Long productId;

    @Min(0)
    private Integer count;
}
