package com.sdu.rabbix.order.entity.vo;

import com.sdu.rabbix.common.domain.po.ProductOrderDetail;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@ToString
public class CreateOrderVO {

    // 用户id
    @NotNull(message = "用户id不能为空")
    private Long accountId;

    // 地址
    @NotBlank(message = "地址不能为空")
    private String address;

    // 订单细节
    @Valid
    @NotEmpty(message = "商品不能为空")
    private List<ProductOrderDetail> details;
}
